package eu.einfracentral.service;

public interface SynchronizerService<T> {

    /**
     * Synchronize resource addition on a different host
     *
     * @param t resource
     */
    void syncAdd(T t);

    /**
     * Synchronize resource update on a different host
     *
     * @param t resource
     */
    void syncUpdate(T t);

    /**
     * Synchronize resource deletion on a different host
     *
     * @param t resource
     */
    void syncDelete(T t);

    /**
     * Synchronize resource verification on a different host
     *
     * @param t resource
     */
    void syncVerify(T t);
}
