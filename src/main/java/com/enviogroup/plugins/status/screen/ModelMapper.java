package com.enviogroup.plugins.status.screen;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.enviogroup.plugins.accountCF.Carrier;
import com.enviogroup.plugins.documentation.IssueWorker;
import com.enviogroup.plugins.status.screen.letters.BaseLetterModel;
import com.enviogroup.plugins.status.screen.letters.InputLetterModel;
import com.enviogroup.plugins.status.screen.letters.OutputLetterModel;

import java.sql.Timestamp;
import java.util.*;

import static com.enviogroup.plugins.status.screen.CustomField.*;

public class ModelMapper {
    private final IssueWorker issueWorker = new IssueWorker();

    public ModelMapper() {
    }

    public TenderModel getTenderModel(String issueId) {
        Issue issue = issueWorker.getIssue(issueId);
        return getTenderModel(issue);
    }

    public TenderModel getTenderModel(Issue issue) {
        if (issue == null) {
            return new TenderModel();
        }
        TenderModel model = new TenderModel();
        model.setKey(issue.getKey());
        model.setSummary(issue.getSummary());
        model.setStatus(issue.getStatus());
        model.setProcedureNumber(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10132, issue));
        model.setBuyAmount(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10522, issue));
        Double newSaleAmount = issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10056, issue);
        model.setUrl(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10047, issue));
        model.setInvoicesList(invoiceModelListFactory(issue, issueWorker, CUSTOM_FIELD_10229));
        if (newSaleAmount == null || newSaleAmount.isNaN()) {
            model.setSaleAmount(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10518, issue));
        } else {
            model.setSaleAmount(newSaleAmount);
        }
        model.setFinanceModel(financeModelFactory(issue, issueWorker));
        List<List<BaseLetterModel>> sortedLetters = sortLetterChain(lettersModelListFactory(issue, issueWorker, CUSTOM_FIELD_11000));
        model.setLettersList(sortedLetters);
        Map<Integer, Object> documentsMap = issueWorker.issueMap(issue, CUSTOM_FIELD_10327);
        for (Map.Entry entry : documentsMap.entrySet()) {
            Issue issueDoc = (MutableIssue) entry.getValue();
            if (!issueDoc.getIssueTypeId().equals(Long.toString(ISSUE_TYPE_ID_DOGOVOR))) {
                continue;
            }
            AgreementModel agreementModel = new AgreementModel();
            Issue org = (issueWorker.getMutableIssuesList(issueDoc, CUSTOM_FIELD_10069)).get(0);
            if (isOrgOurs(org)) {
                org = (issueWorker.getMutableIssuesList(issueDoc, CUSTOM_FIELD_10067)).get(0);
                model.setAgreement(agreementModel);
            } else {
                model.addAgreement(agreementModel);
            }
            OrganisationModel organisationModel = organisationModelFactory(org, issueWorker);
            String vatString = issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10113, issueDoc);
            double vatDouble;
            try {
                vatDouble = Double.parseDouble(vatString) / 100;
            } catch (Exception e) {
                vatDouble = 0D;
            }
            agreementModel.setValueAddedTax(vatDouble);
            agreementModel.setOrganisation(organisationModel);
            agreementModel.setAmount((issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10072, issueDoc)));
            agreementModel.setKey(issueDoc.getKey());
            agreementModel.setStatus(issueDoc.getStatus());
            agreementModel.setSummary(issueDoc.getSummary());
            agreementModel.setSpecificationsList(specificationModelListFactory(issueDoc, issueWorker));
            agreementModel.setInputInvoicesList(invoiceModelListFactory(issueDoc, issueWorker, CUSTOM_FIELD_11201));
            setAgreementAlarm(agreementModel);
            agreementModel.setShipmentsList(shipmentModelListFactory(issueDoc, issueWorker));
        }
        try {
            editModelAmountWithVAT(model);
        } catch (Exception ignored) {

        }
        return model;
    }

    public AgreementModel getAgreementModel(Issue issue) {
        if (issue == null) {
            return new AgreementModel();
        }
        AgreementModel model = new AgreementModel();
        model.setTender(getTenderModel( issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10326).get(0)));
        model.setKey(issue.getKey());
        model.setSummary(issue.getSummary());
        model.setStatus(issue.getStatus());
        Issue org = (issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10069)).get(0);
        if (isOrgOurs(org)) {
            org = (issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10067)).get(0);
        }
        model.setOrganisation(organisationModelFactory(org, issueWorker));
        String vatString = issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10113, issue);
        double vatDouble;
        try {
            vatDouble = Double.parseDouble(vatString) / 100;
        } catch (Exception e) {
            vatDouble = 0D;
        }
        model.setValueAddedTax(vatDouble);
        model.setAmount((issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10072, issue)));
        model.setSpecificationsList(specificationModelListFactory(issue, issueWorker));
        model.setInputInvoicesList(invoiceModelListFactory(issue, issueWorker, CUSTOM_FIELD_11201));
        model.setShipmentsList(shipmentModelListFactory(issue, issueWorker));
        return model;
    }

    public List<BaseLetterModel> lettersModelListFactory(Issue issue, IssueWorker issueWorker, Long customFieldId) {
        ArrayList<MutableIssue> mutableLettersList = issueWorker.getMutableIssuesList(issue, customFieldId);
        HashMap<String, BaseLetterModel> lettersHashMap = new HashMap<>();
        if (!mutableLettersList.isEmpty()) {
            List<BaseLetterModel> lettersList = new LinkedList<>();
            for (Issue letterIssue : mutableLettersList) {
                String letterTypeId = Objects.requireNonNull(letterIssue.getIssueType()).getId();
                if (letterTypeId.equals(INPUT_LETTER_ISSUE_TYPE_ID)) {
                    InputLetterModel inputLetter = inputLetterModelFactory(lettersHashMap, letterIssue, null);
                    if (inputLetter != null) {
                        lettersList.add(inputLetter);
                    }
                } else if (letterTypeId.equals(OUTPUT_LETTER_ISSUE_TYPE_ID)) {
                    OutputLetterModel outputLetter = outputLetterModelFactory(lettersHashMap, letterIssue, null);
                    if (outputLetter != null) {
                        lettersList.add(outputLetter);
                    }
                }
            }
            return lettersList;
        } else {
            return null;
        }
    }

    private final List<String> letterModels = new ArrayList<>();

    private OutputLetterModel outputLetterModelFactory(HashMap<String, BaseLetterModel> lettersHashMap, Issue issue, InputLetterModel parentModel) {
        if (lettersHashMap.containsKey(issue.getKey())) {
            return null;
        }
        OutputLetterModel outputLetter = new OutputLetterModel();
        outputLetter.setKey(issue.getKey());
        outputLetter.setStatus(issue.getStatus());
        outputLetter.setSummary(issue.getSummary());
        outputLetter.setCreated(issue.getCreated());
        outputLetter.setResolution(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10607, issue));
        outputLetter.setOutNumber(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10208, issue));
        outputLetter.setInNumber(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10207, issue));
        outputLetter.setType(issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10210, issue));
        for (Issue org : issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10502)) {
            outputLetter.addOrganisation(organisationModelFactory(org, issueWorker));
        }

        lettersHashMap.put(outputLetter.getKey(), outputLetter);
        if (parentModel != null) {
            outputLetter.setParentLetter(parentModel);
        } else if (!issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10541).isEmpty()) {
            outputLetter.setParentLetter(inputLetterModelFactory(lettersHashMap, issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10541).get(0), null));
        }
        if (!issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_12300).isEmpty()) {
            outputLetter.setChildLetter(inputLetterModelFactory(lettersHashMap, issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_12300).get(0), outputLetter));
        }
        return outputLetter;
    }

    private InputLetterModel inputLetterModelFactory(HashMap<String, BaseLetterModel> lettersHashMap, Issue issue, OutputLetterModel parentModel) {
        if (lettersHashMap.containsKey(issue.getKey())) {
            return null;
        }
        InputLetterModel inputLetter = new InputLetterModel();
        inputLetter.setKey(issue.getKey());
        inputLetter.setStatus(issue.getStatus());
        inputLetter.setSummary(issue.getSummary());
        inputLetter.setCreated(issue.getCreated());
        inputLetter.setInNumber(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10208, issue));
        inputLetter.setOutNumber(issueWorker.getStringCustomFieldValue(CUSTOM_FIELD_10207, issue));
        inputLetter.setType(issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10210, issue));
        for (Issue org : issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10504)) {
            inputLetter.addOrganisation(organisationModelFactory(org, issueWorker));
        }
        lettersHashMap.put(inputLetter.getKey(), inputLetter);
        if (parentModel != null) {
            inputLetter.setParentLetter(parentModel);
        } else if (!issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_12301).isEmpty()) {
            inputLetter.setParentLetter(outputLetterModelFactory(lettersHashMap, issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_12301).get(0), null));
        }
        if (!issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10542).isEmpty()) {
            inputLetter.setChildLetter(outputLetterModelFactory(lettersHashMap, issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_10542).get(0), inputLetter));
        }
        return inputLetter;
    }

    private List<InvoiceModel> invoiceModelListFactory(Issue issue, IssueWorker issueWorker, Long cfId) {
        List<InvoiceModel> invoiceList = new ArrayList<>();
        if (!issueWorker.getMutableIssuesList(issue, cfId).isEmpty()) {
            for (Issue invoiceIssue : (issueWorker.getMutableIssuesList(issue, cfId))) {
                InvoiceModel invoiceModel = new InvoiceModel();
                invoiceModel.setKey(invoiceIssue.getKey());
                invoiceModel.setStatus(invoiceIssue.getStatus());
                invoiceModel.setSummary(invoiceIssue.getSummary());
                invoiceModel.setAmount((issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10085, invoiceIssue)));
                invoiceList.add(invoiceModel);
                invoiceModel.setDetailedInformation((Collection<Carrier>) issueWorker.getObjectCustomFieldValue(CUSTOM_FIELD_11300, invoiceIssue));
                invoiceModel.setType(issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10543, invoiceIssue));
                try {
                    Issue org = issueWorker.getMutableIssuesList(invoiceIssue, CUSTOM_FIELD_10339).get(0);
                    invoiceModel.setOrganisation(organisationModelFactory(org, issueWorker));
                } catch (Exception e) {
                    invoiceModel.setOrganisation(null);
                }
            }
            return invoiceList;
        } else {
            return null;
        }
    }

    public List<SpecificationModel> specificationModelListFactory(Issue agreementIssue, IssueWorker issueWorker) {
        List<SpecificationModel> specificationList = new ArrayList<>();
        if (!issueWorker.getMutableIssuesList(agreementIssue, CUSTOM_FIELD_10107).isEmpty()) {
            for (Issue specificationIssue : (issueWorker.getMutableIssuesList(agreementIssue, CUSTOM_FIELD_10107))) {
                SpecificationModel specificationModel = new SpecificationModel();
                specificationModel.setKey(specificationIssue.getKey());
                specificationModel.setStatus(specificationIssue.getStatus());
                specificationModel.setSummary(specificationIssue.getSummary());
                specificationModel.setAmount((issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10108, specificationIssue)));
                specificationModel.setInvoiceModelList(invoiceModelListFactory(specificationIssue, issueWorker, CUSTOM_FIELD_10356));
                specificationModel.setDetailedInformation((Collection<Carrier>) issueWorker.getObjectCustomFieldValue(CUSTOM_FIELD_12500, specificationIssue));
                specificationModel.setDeliveryTime((Timestamp) issueWorker.getObjectCustomFieldValue(CUSTOM_FIELD_10112, specificationIssue));
                try {
                    Issue org = isOrgOurs((issueWorker.getMutableIssuesList(specificationIssue, CUSTOM_FIELD_10341)).get(0)) ? issueWorker.getMutableIssuesList(specificationIssue, CUSTOM_FIELD_10343).get(0) : issueWorker.getMutableIssuesList(specificationIssue, CUSTOM_FIELD_10341).get(0);
                    specificationModel.setOrganisation(organisationModelFactory(org, issueWorker));
                } catch (Exception e) {
                    specificationModel.setOrganisation(null);
                }
                specificationList.add(specificationModel);
            }
            return specificationList;
        } else {
            return null;
        }
    }

    public List<ShipmentModel> shipmentModelListFactory(Issue agreementIssue, IssueWorker issueWorker) {
        List<ShipmentModel> shipmentModelList = new ArrayList<>();
        if (!issueWorker.getMutableIssuesList(agreementIssue, CUSTOM_FIELD_10099).isEmpty()) {
            for (Issue shipmentIssue : (issueWorker.getMutableIssuesList(agreementIssue, CUSTOM_FIELD_10099))) {
                ShipmentModel shipmentModel = new ShipmentModel();
                shipmentModel.setKey(shipmentIssue.getKey());
                shipmentModel.setStatus(shipmentIssue.getStatus());
                shipmentModel.setSummary(shipmentIssue.getSummary());
                try {
                    Issue org = isOrgOurs((issueWorker.getMutableIssuesList(shipmentIssue, CUSTOM_FIELD_10346)).get(0)) ? issueWorker.getMutableIssuesList(shipmentIssue, CUSTOM_FIELD_10348).get(0) : issueWorker.getMutableIssuesList(shipmentIssue, CUSTOM_FIELD_10346).get(0);
                    shipmentModel.setOrganisation(organisationModelFactory(org, issueWorker));
                } catch (Exception e) {
                    shipmentModel.setOrganisation(null);
                }
                shipmentModelList.add(shipmentModel);
            }
            return shipmentModelList;
        } else {
            return null;
        }
    }

    public OrganisationModel organisationModelFactory(Issue orgIssue, IssueWorker issueWorker) {
        OrganisationModel organisationModel = new OrganisationModel();
        organisationModel.setKey(orgIssue.getKey());
        organisationModel.setSummary(orgIssue.getSummary());
        String orgStatus = issueWorker.getStringValueFromLazyLoadedOptionCustomField(CUSTOM_FIELD_10121, orgIssue);
        if (orgStatus != null) {
            organisationModel.setOrgStatus(orgStatus);
            switch (orgStatus) {
                case ORGANISATION_STATUS_APPROVED:
                    organisationModel.setStatusColor(ORGANISATION_STATUS_APPROVED_STATUS);
                    break;
                case ORGANISATION_STATUS_NOT_APPROVED:
                    organisationModel.setStatusColor(ORGANISATION_STATUS_NOT_APPROVED_STATUS);
                    break;
                case ORGANISATION_STATUS_NOT_VELIDATED:
                    organisationModel.setStatusColor(ORGANISATION_STATUS_NOT_VELIDATED_STATUS);
                    break;
                default:
                    organisationModel.setStatusColor(ORGANISATION_STATUS_DEFAULT_STATUS);
                    break;
            }
        } else {
            organisationModel.setOrgStatus(ORGANISATION_STATUS_NOT_SET);
            organisationModel.setStatusColor(ORGANISATION_STATUS_NOT_SET_STATUS);
        }
        return organisationModel;
    }

    public FinanceModel financeModelFactory(Issue tenderIssue, IssueWorker issueWorker) {
        FinanceModel financeModel = new FinanceModel();
        financeModel.setAdditionalExpenses(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_12101, tenderIssue));
        financeModel.setFinanceExpenses(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10515, tenderIssue));
        financeModel.setLogisticExpenses(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10513, tenderIssue));
        financeModel.setTravelExpenses(issueWorker.getDoubleCustomFieldValue(CUSTOM_FIELD_10514, tenderIssue));
        return financeModel;
    }


    private boolean isOrgOurs(Issue org) {
        return org.getKey().equals(ORG_1) || org.getKey().equals(ORG_2365);
    }

    private void editModelAmountWithVAT(TenderModel model) {
        if (model.getAgreement() != null && model.getAgreement().getValueAddedTax() != 0) {
            Double agreementAmount = model.getAgreement().getAmount();
            model.getAgreement().setAmount(agreementAmount * VAT_COEFFICIENT);
            if (model.getAgreement().getSpecificationsList() != null && !model.getAgreement().getSpecificationsList().isEmpty()) {
                for (SpecificationModel sp : model.getAgreement().getSpecificationsList()) {
                    Double specificationAmount = sp.getAmount();
                    if (specificationAmount != null) {
                        sp.setAmount(specificationAmount * VAT_COEFFICIENT);
                    }
                }
            }
        }
        if (model.getAgreementsList() != null && !model.getAgreementsList().isEmpty()) {
            for (AgreementModel am : model.getAgreementsList()) {
                if (am.getValueAddedTax() != 0) {
                    Double agreementAmount = am.getAmount();
                    if (agreementAmount != null) {
                        am.setAmount(agreementAmount * VAT_COEFFICIENT);
                    }
                    if (am.getSpecificationsList() != null && !am.getSpecificationsList().isEmpty()) {
                        for (SpecificationModel sp : am.getSpecificationsList()) {
                            Double specificationAmount = sp.getAmount();
                            if (specificationAmount != null) {
                                sp.setAmount(specificationAmount * VAT_COEFFICIENT);
                            }
                        }
                    }
                }
            }
        }
//        if (am.getInputInvoicesList() != null) {
//            for (InvoiceModel in : am.getInputInvoicesList()) {
//                amount = in.getAmount();
//                if (amount != null) {
//                    in.setAmount(amount * VAT_COEFFICIENT);
//                }
//            }
//        }
    }

    private void setAgreementAlarm(AgreementModel agreementModel) {
        agreementModel.setAlarm(false);
        try {
            if (agreementModel.getInputInvoicesList() != null && agreementModel.getSpecificationsList() != null) {
                for (SpecificationModel specification : agreementModel.getSpecificationsList()) {
                    for (InvoiceModel invoice : agreementModel.getInputInvoicesList()) {
                        if (specification.getOrganisation().equals(invoice.getOrganisation())) {
                            agreementModel.setAlarm(true);
                        }
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Сортирует список цепочек по количесту писем в ней
     *
     * @param letterModels Список цепочек пиьсем
     * @return Отсортированный список
     */
    private List<List<BaseLetterModel>> sortLetterChain(List<BaseLetterModel> letterModels) {
        if (letterModels == null) {
            return null;
        }
        letterModels.replaceAll(BaseLetterModel::getFirstParent);
        letterModels.sort(Comparator.comparingLong(BaseLetterModel::getLongCreated));
        List<List<BaseLetterModel>> lettersChains = new ArrayList<>();
        for (BaseLetterModel letterModel : letterModels) {
            List<BaseLetterModel> chain = new LinkedList<>();
            while (letterModel != null) {
                chain.add(BaseLetterModel.createBaseLetterModel(letterModel));
                letterModel = letterModel.getChildLetter();
            }
            lettersChains.add(chain);
        }
        return lettersChains;
    }
}

