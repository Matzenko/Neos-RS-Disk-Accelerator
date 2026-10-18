package de.neoscolony.rsaccelerator.mixin;
import de.neoscolony.rsaccelerator.Accelerator;
import com.refinedmods.refinedstorage.api.storage.Storage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the original Disk Interface item quota. No transfers, UUIDs or inventories are replaced. */
@Mixin(targets="com.refinedmods.refinedstorage.common.storage.diskinterface.AbstractDiskInterfaceBlockEntity",remap=false)
public abstract class DiskInterfaceQuotaMixin {
 @Inject(method="lambda$new$1(Lcom/refinedmods/refinedstorage/api/storage/Storage;)J",at=@At("RETURN"),cancellable=true,remap=false,require=1)
 private void neos$quota(Storage storage,CallbackInfoReturnable<Long> result){
  result.setReturnValue(Accelerator.quota(this,storage,result.getReturnValue()));
 }
}