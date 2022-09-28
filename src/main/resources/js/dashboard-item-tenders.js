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

            this.getIssues(self.getIssues(data.issues)).done(function (dataModel) {
                self.dataModelIssues = dataModel.issues;

                if (self.dataModelIssues === undefined || self.dataModelIssues.length  === 0) {
                    $element.empty().html(Dashboard.Item.Tenders.Templates.Empty());
                }
                else {
                    $element.empty().html(Dashboard.Item.Tenders.Templates.IssueList({issues: self.dataModelIssues}));
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
            url: contextPath() + "/rest/api/2/search?jql=project %3D CRM AND issuetype = Тендеры AND Status changed from \"Подготовка документации\" to \"Тендер подан\" during (-" + preferences['due-date-input'] + ", now()) "
        });
    };

    DashboardItem.prototype.renderEdit = function (context, preferences) {
        var $element = this.$element = $(context).find("#dynamic-content");
        $element.empty().html(Dashboard.Item.Tenders.Templates.Configuration());
        this.API.once("afterRender", this.API.resize);
        var $form = $("form", $element);
        $(".cancel", $form).click(_.bind(function() {
            if(preferences['due-date-input'])
                this.API.closeEdit();
        }, this));

        $form.submit(_.bind(function(event) {
            event.preventDefault();

            var preferences = getPreferencesFromForm($form);
            var regexp = /^\d+([dwm])$/;
            if(regexp.test(preferences['due-date-input'])) {
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

        preferencesArray.forEach(function(element) {
            preferencesObject[element.name] = element.value;
        });

        return preferencesObject;
    }
    return DashboardItem;
});

// Put somewhere in your scripting environment
if (jQuery.when.all===undefined) {
    jQuery.when.all = function(deferreds) {
        var deferred = new jQuery.Deferred();
        $.when.apply(jQuery, deferreds).then(
            function() {
                deferred.resolve(Array.prototype.slice.call(arguments));
            },
            function() {
                deferred.fail(Array.prototype.slice.call(arguments));
            });

        return deferred;
    }
}