package eu.einfracentral.service;

public interface SynchronizerService<T> {

    void syncAdd(T t);

    void syncUpdate(T t);

    void syncDelete(T t);

    void syncVerify(T t);
}
