package eu.einfracentral.service;

public interface SynchronizerService<T> {

    void syncAdd(T t);

    void syncUpdate(T t, T previous);

    void syncDelete(T t);

    void syncAll();
}
