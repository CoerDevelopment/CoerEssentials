package de.coerdevelopment.essentials.job;

public abstract class Job {

    protected final String name;
    protected final String description;


    public Job(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected abstract void before(JobExecution execution);

    protected abstract void execute(JobExecution execution);

    protected abstract void finish(JobExecution execution);

    public abstract JobOptions getDefaultOptions();

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
