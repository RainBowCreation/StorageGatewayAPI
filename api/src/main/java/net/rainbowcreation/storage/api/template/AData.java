package net.rainbowcreation.storage.api.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.rainbowcreation.storage.template.IDataManager;

public abstract class AData<T extends AData<T>> {

    @JsonIgnore
    protected transient IDataManager dm;

    /** Per-entity: storage namespace, e.g. "userById". */
    protected abstract String ns();

    /** Per-entity: unique key, e.g. uid. */
    protected abstract String key();

    /** Attach a dm so save() works after loading. */
    @SuppressWarnings("unchecked")
    public T with(IDataManager dm) { this.dm = dm; return (T) this; }

    /** Persist this instance. */
    protected boolean save() {
        if (dm != null) dm.set(ns(), key(), this);
        return true;
    }

    /** delete from sgw */
    protected boolean delete() {
        if (dm != null) dm.delete(ns(), key());
        return true;
    }

    /** Generic load that also re-attaches dm. */
    protected static <R extends AData<R>> R load(IDataManager dm, Class<R> type, String ns, String key) {
        R r = dm.get(ns, key, type);
        if (r != null) r.with(dm);
        return r;
    }
}