package de.neoscolony.rsaccelerator.mixin;
import com.refinedmods.refinedstorage.api.storage.Storage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A zero budget means no transfer, never a zero-amount call into RS storage. */
@Mixin(targets="com.refinedmods.refinedstorage.api.network.impl.node.storagetransfer.StorageTransferNetworkNode",remap=false)
public abstract class EmptyBudgetMixin {
 @Inject(method="transfer(Lcom/refinedmods/refinedstorage/api/storage/Storage;Lcom/refinedmods/refinedstorage/api/storage/Storage;J)Z",at=@At("HEAD"),cancellable=true,remap=false,require=1)
 private void neos$emptyBudget(Storage source,Storage target,long amount,CallbackInfoReturnable<Boolean> result){
  if(amount==0)result.setReturnValue(false);
 }
}