define('dashboard-items/tenders', ['underscore', 'jquery', 'wrm/context-path'], function (_, $, contextPath) {
    var DashboardItem = function (API) {
        this.API = API;
        this.issues = [];
        this.items = [];
    };
    /**
     * Called to render the view for a fully configured dashboard item.
     *
     * @param context The surrounding <div/> context that this items should render into.
     * @param preferences The user preferences saved for this dashboard item (e.g. filter id, number of results...)
     */
    DashboardItem.prototype.render = function (context, preferences) {
        this.API.showLoadingBar();
        var $element = this.$element = $(context).find("#dynamic-content");
        var self = this;
        this.requestData(preferences).done(function (data) {
            self.API.hideLoadingBar();

            self.issues = data.issues;

            self.getIssues(data.issues).done(function (dataModel) {
                self.dataModelIssues = dataModel.tenders;

                if (self.dataModelIssues === undefined || self.dataModelIssues.length === 0) {
                    $element.empty().html(Dashboard.Item.Tenders.Templates.Empty());
                } else {

                    var formatCurrency = new Intl.NumberFormat('ru', {
                            style: 'currency',
                            currency: 'RUB',
                            minimumFractionDigits: 2
                        });

                    var data = {
                        sumBuyAmount: 0,
                        sumSaleAmount: 0,
                        sumLogisticExpenses: 0,
                        sumFinanceExpenses: 0,
                        sumTravelExpenses: 0,
                        sumAdditionalExpenses: 0,
                        totalExpenses: 0
                    };
                    for (var issueItemId in self.dataModelIssues) {
                        var issueItem = self.dataModelIssues[issueItemId];
                        data.sumBuyAmount = data.sumBuyAmount + (!issueItem.hasOwnProperty('buyAmount') ? 0 : issueItem.buyAmount);
                        data.sumSaleAmount = data.sumSaleAmount + (!issueItem.hasOwnProperty('saleAmount') ? 0 : issueItem.saleAmount);
                        if (issueItem.hasOwnProperty('financeModel')) {
                            data.sumLogisticExpenses = data.sumLogisticExpenses + (!issueItem.financeModel.hasOwnProperty('logisticExpenses') ? 0 : issueItem.financeModel.logisticExpenses);
                            data.sumFinanceExpenses = data.sumFinanceExpenses + (!issueItem.financeModel.hasOwnProperty('financeExpenses') ? 0 : issueItem.financeModel.financeExpenses);
                            data.sumTravelExpenses = data.sumTravelExpenses + (!issueItem.financeModel.hasOwnProperty('travelExpenses') ? 0 : issueItem.financeModel.travelExpenses);
                            data.sumAdditionalExpenses = data.sumAdditionalExpenses + (!issueItem.financeModel.hasOwnProperty('additionalExpenses') ? 0 : issueItem.financeModel.additionalExpenses);
                            issueItem.financeModel.logisticExpenses = formatCurrency.format(issueItem.financeModel.logisticExpenses);
                            issueItem.financeModel.financeExpenses = formatCurrency.format(issueItem.financeModel.financeExpenses);
                            issueItem.financeModel.travelExpenses = formatCurrency.format(issueItem.financeModel.travelExpenses);
                            issueItem.financeModel.additionalExpenses = formatCurrency.format(issueItem.financeModel.additionalExpenses);
                        }
                        issueItem.buyAmount = formatCurrency.format(issueItem.buyAmount);
                        issueItem.saleAmount = formatCurrency.format(issueItem.saleAmount);

                    }
                    data.totalExpenses = data.totalExpenses + data.sumBuyAmount + data.sumLogisticExpenses + data.sumFinanceExpenses + data.sumTravelExpenses;
                    data.sumBuyAmount = formatCurrency.format(data.sumBuyAmount);
                    data.sumSaleAmount = formatCurrency.format(data.sumSaleAmount);
                    data.sumLogisticExpenses = formatCurrency.format(data.sumLogisticExpenses);
                    data.sumFinanceExpenses = formatCurrency.format(data.sumFinanceExpenses);
                    data.sumTravelExpenses = formatCurrency.format(data.sumTravelExpenses);
                    data.totalExpenses = formatCurrency.format(data.totalExpenses);

                    $element.empty().html(Dashboard.Item.Tenders.Templates.IssueList({
                        issues: self.dataModelIssues.reduce(function(result, value, index, array) {
                            if (index % 2 === 0)
                                result.push(array.slice(index, index + 2));
                            return result;
                        }, []),
                        data: data,
                        context: contextPath()
                    }));
                }
                self.API.resize();
                $element.find(".submit").click(function (event) {
                    event.preventDefault();
                    self.render(element, preferences);
                });
            });
        });

        this.API.once("afterRender", this.API.resize);
    };

    DashboardItem.prototype.requestData = function (preferences) {
        return $.ajax({
            method: "GET",
            //url: contextPath() + "/rest/api/2/search?jql=project %3D CRM AND issuetype = Тендеры AND Status changed from \"Подготовка документации\" to \"Тендер подан\" during (-" + preferences['due-date-input'] + ", now()) "
            url: contextPath() + "/rest/api/2/search?fields=key&maxResults=1000&jql=project %3D CRM AND issuetype = Тендеры AND (status = Выиграли OR status = \"Контракт закрыт\") AND \"Дата подведения итогов\" >= \"2024/01/01\" ORDER BY cf[10049] ASC"
        });
    };


    DashboardItem.prototype.renderEdit = function (context, preferences) {
        var $element = this.$element = $(context).find("#dynamic-content");
        $element.empty().html(Dashboard.Item.Tenders.Templates.Configuration());
        this.API.once("afterRender", this.API.resize);
        var $form = $("form", $element);
        $(".cancel", $form).click(_.bind(function () {
            if (preferences['due-date-input'])
                this.API.closeEdit();
        }, this));

        $form.submit(_.bind(function (event) {
            event.preventDefault();

            var preferences = getPreferencesFromForm($form);
            var regexp = /^\d+([dwm])$/;
            if (regexp.test(preferences['due-date-input'])) {
                this.API.savePreferences(preferences);
                this.API.showLoadingBar();
            }
        }, this));
    };

    DashboardItem.prototype.getIssues = function (issues) {
        var issueIds = [];
        for (item in issues) {
            var issueId = issues[item].key;
            issueIds.push(issueId);
        }
        return $.ajax({
            url: contextPath() + "/rest/tendersrestresource/1/tenders.json",
            dataType: 'json',
            data: {
                keys: issueIds.join(',')
            },
            method: 'POST'
        });
    }

    function getPreferencesFromForm($form) {
        var preferencesArray = $form.serializeArray();
        var preferencesObject = {};

        preferencesArray.forEach(function (element) {
            preferencesObject[element.name] = element.value;
        });

        return preferencesObject;
    }

    return DashboardItem;
});

// Put somewhere in your scripting environment
if (jQuery.when.all === undefined) {
    jQuery.when.all = function (deferreds) {
        var deferred = new jQuery.Deferred();
        $.when.apply(jQuery, deferreds).then(
            function () {
                deferred.resolve(Array.prototype.slice.call(arguments));
            },
            function () {
                deferred.fail(Array.prototype.slice.call(arguments));
            });

        return deferred;
    }
}