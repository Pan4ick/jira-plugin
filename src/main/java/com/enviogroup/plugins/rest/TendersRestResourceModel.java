package com.enviogroup.plugins.rest;

import com.enviogroup.plugins.status.screen.ModelMapper;
import com.enviogroup.plugins.status.screen.TenderModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "message")
@XmlAccessorType(XmlAccessType.FIELD)
public class TendersRestResourceModel {
    @XmlElement(name = "tenders")
    private List<TenderModel> tenders;

    @XmlAttribute
    private String key;

    public TendersRestResourceModel() {
    }

    public TendersRestResourceModel(List<String> issueIds) {
        ModelMapper modelMapper = new ModelMapper();
        List<TenderModel> issues = new ArrayList<>();
        for (String issueId : issueIds) {
            issues.add(modelMapper.getTenderModel(issueId));
        }
        this.tenders = issues;
    }

    public TendersRestResourceModel(String issueId) {
        this.key = issueId;
    }

    public List<TenderModel> getTenders() {
        return tenders;
    }

    public void setTenders(List<TenderModel> tenders) {
        this.tenders = tenders;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}