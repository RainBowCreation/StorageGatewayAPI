package net.rainbowcreation.storage.api;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;

public final class SgwRepository<T> {
    private final SgwContext ctx;
    private final Class<T> type;
    private final String ns;
    private final Function<T,String> keyFn;
    private final BiConsumer<T,SgwContext> afterLoad;
    private final Duration getTimeout;

    private SgwRepository(Builder<T> b){
        this.ctx=b.ctx; this.type=b.type; this.ns=b.ns; this.keyFn=b.keyFn;
        this.afterLoad=b.afterLoad!=null? b.afterLoad: (x,c)->{};
        this.getTimeout=b.getTimeout!=null? b.getTimeout: Duration.ofMillis(500);
    }

    public CompletableFuture<Void> save(T entity){
        return ctx.client().set(ctx.fullNs(ns), keyFn.apply(entity), entity);
    }
    public CompletableFuture<Optional<T>> get(String key){
        return ctx.client().get(ctx.fullNs(ns), key, type).thenApply(opt -> {
            opt.ifPresent(v -> afterLoad.accept(v, ctx));
            return opt;
        });
    }
    public T getBlocking(String key){
        T v = ctx.client().getBlocking(ctx.fullNs(ns), key, type, getTimeout);
        if (v!=null) afterLoad.accept(v, ctx);
        return v;
    }

    public static <U> Builder<U> forType(Class<U> type){ return new Builder<>(type); }

    public static final class Builder<U>{
        private final Class<U> type;
        private SgwContext ctx; private String ns; private Function<U,String> keyFn;
        private BiConsumer<U,SgwContext> afterLoad; private Duration getTimeout;
        private Builder(Class<U> t){ this.type=t; }
        public Builder<U> context(SgwContext c){ this.ctx=c; return this; }
        public Builder<U> namespace(String n){ this.ns=n; return this; }
        public Builder<U> key(Function<U,String> k){ this.keyFn=k; return this; }
        public Builder<U> afterLoad(BiConsumer<U,SgwContext> a){ this.afterLoad=a; return this; }
        public Builder<U> timeout(Duration d){ this.getTimeout=d; return this; }
        public SgwRepository<U> build(){ return new SgwRepository<>(this); }
    }
}

