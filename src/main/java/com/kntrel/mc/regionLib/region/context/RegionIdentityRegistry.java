package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.region.Region;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class RegionIdentityRegistry {

    private static final class IdentityRef extends WeakReference<Region> {
        private final long id_;

        private IdentityRef(long id, Region referent, ReferenceQueue<Region> queue) {
            super(referent, queue);
            this.id_ = id;
        }
    }


    //FIELDS
    private final Map<Long, IdentityRef> regions_;
    private final ReferenceQueue<Region> queue_;


    //CONSTRUCTOR
    RegionIdentityRegistry() {
        this.regions_ = new HashMap<>();
        this.queue_ = new ReferenceQueue<>();
    }


    //API
    public synchronized Optional<Region> get(long id) {
        this.collectGarbage();
        IdentityRef ref = this.regions_.get(id);
        if (ref == null) { return Optional.empty(); }
        Region region = ref.get();
        if (region != null) { return Optional.of(region); }
        this.regions_.remove(id, ref);
        return Optional.empty();
    }
    public synchronized Region canonicalize(Region region) {
        Long id = region.getId();
        if (id == null) { return region; }

        this.collectGarbage();
        IdentityRef ref = this.regions_.get(id);
        Region existing = (ref == null) ? null : ref.get();
        if (existing != null) {
            return existing;
        }

        this.regions_.put(id, new IdentityRef(id, region, this.queue_));
        return region;
    }


    //PRIVATE
    private void collectGarbage() {
        IdentityRef ref;
        while ((ref = (IdentityRef) this.queue_.poll()) != null) {
            this.regions_.remove(ref.id_, ref);
        }
    }
}
