package com.katza.myfirestoreapp;

@com.google.firebase.firestore.IgnoreExtraProperties
public class Project {
    private String name = "";
    private String description = "";
    private String category = "";
    private String status = "";
    private String nextStep = "";
    private String repositoryUrl = "";
    private String journal = "";

    public Project() { }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getNextStep() {
        return nextStep;
    }
    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

}
