package com.enviogroup.plugins.status.screen.letters;

import com.enviogroup.plugins.status.screen.IssueModel;
import com.enviogroup.plugins.status.screen.OrganisationModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public class BaseLetterModel extends IssueModel {
    private Timestamp created;
    private List<OrganisationModel> organisations;
    private String resolution;
    private String type;
    private String inNumber;
    private String outNumber;

    public BaseLetterModel() {

    }


    private BaseLetterModel(BaseLetterModel that) {
        this.childLetter = that.getChildLetter();
        this.parentLetter = that.getParentLetter();
        this.letterType = that.getLetterType();
        this.setKey(that.getKey());
        this.setSummary(that.getSummary());
        this.setStatus(that.getStatus());
        this.created = that.getCreated();
        this.organisations = that.getOrganisations();
        this.resolution = that.getResolution();
        this.type = that.getType();
        this.inNumber = that.getInNumber();
        this.outNumber = that.getOutNumber();
    }

    private String letterType;

    public static BaseLetterModel createBaseLetterModel(BaseLetterModel that) {
        return new BaseLetterModel(that);
    }

    public String getLetterType() {
        return letterType;
    }

    public void setLetterType(String letterType) {
        this.letterType = letterType;
    }

    private BaseLetterModel childLetter;

    private BaseLetterModel parentLetter;

    public BaseLetterModel getChildLetter() {
        return childLetter;
    }

    public void setChildLetter(BaseLetterModel childLetter) {
        this.childLetter = childLetter;
    }

    public BaseLetterModel getParentLetter() {
        return parentLetter;
    }

    public void setParentLetter(BaseLetterModel parentLetter) {
        this.parentLetter = parentLetter;
    }

    public int getChildCount() {
        BaseLetterModel letterModel = this;
        int i = 0;
        while (letterModel.getChildLetter() != null) {
            letterModel = letterModel.getChildLetter();
            i++;
        }
        return i;
    }

    public int getParentCount() {
        BaseLetterModel letterModel = this;
        int i = 0;
        while (letterModel.getParentLetter() != null) {
            letterModel = letterModel.getParentLetter();
            i++;
        }
        return i;
    }

    public int getSize() {
        return getChildCount() + getParentCount();
    }

    public BaseLetterModel getFirstParent() {
        BaseLetterModel letterModel = this;
        while (letterModel.getParentLetter() != null) {
            letterModel = letterModel.getParentLetter();
        }
        return letterModel;
    }

    public BaseLetterModel getLastChild() {
        BaseLetterModel letterModel = this;
        while (letterModel.getChildLetter() != null) {
            letterModel = letterModel.getChildLetter();
        }
        return letterModel;
    }

//    LinkedList<BaseLetterModel> letterChain = new LinkedList<>();
//    public List<BaseLetterModel> getChainAsLinkedList() {
//        BaseLetterModel letterModel = this;
//
//        letterChain.add(letterModel);
//        while (letterModel.getChildLetter() != null)
//    }

    public String getCreatedString() {
        if (created == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new Date(this.created.getTime()));
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public long getLongCreated() {
        if (created == null) {
            return 0;
        }
        return created.getTime();
    }

    public List<OrganisationModel> getOrganisations() {
        return organisations;
    }

    public void setOrganisations(List<OrganisationModel> organisations) {
        this.organisations = organisations;
    }

    public void addOrganisation(OrganisationModel organisation) {
        if (this.organisations == null) {
            this.organisations = new ArrayList<>();
        }
        organisations.add(organisation);
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInNumber() {
        return inNumber;
    }

    public void setInNumber(String inNumber) {
        this.inNumber = inNumber;
    }

    public String getOutNumber() {
        return outNumber;
    }

    public void setOutNumber(String outNumber) {
        this.outNumber = outNumber;
    }
}
