package de.neoscolony.rsaccelerator.mixin;
import de.neoscolony.rsaccelerator.Diagnostics;
import com.refinedmods.refinedstorage.api.storage.Storage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inactive by default. Observes only the selected node during an admin measurement. */
@Mixin(targets="com.refinedmods.refinedstorage.api.network.impl.node.storagetransfer.StorageTransferNetworkNode",remap=false)
public abstract class TransferDiagnosticsMixin {
 @Unique private long neos$before,neos$start;
 @Unique private boolean neos$measuring;
 @Inject(method="transfer(Lcom/refinedmods/refinedstorage/api/storage/Storage;Lcom/refinedmods/refinedstorage/api/storage/Storage;J)Z",at=@At("HEAD"),remap=false,require=1)
 private void neos$beforeTransfer(Storage source,Storage target,long amount,CallbackInfoReturnable<Boolean> result){
  neos$measuring=Diagnostics.watching(this) && amount>0;
  if(neos$measuring){neos$before=source.getStored();neos$start=System.nanoTime();}
 }
 @Inject(method="transfer(Lcom/refinedmods/refinedstorage/api/storage/Storage;Lcom/refinedmods/refinedstorage/api/storage/Storage;J)Z",at=@At("RETURN"),remap=false,require=1)
 private void neos$afterTransfer(Storage source,Storage target,long amount,CallbackInfoReturnable<Boolean> result){
  if(neos$measuring){neos$measuring=false;Diagnostics.transferred(this,Math.max(0,neos$before-source.getStored()),System.nanoTime()-neos$start);}
 }
}