package org.multibit.mbm.resources.purchaseorder;

import com.google.common.base.Optional;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;
import org.multibit.mbm.api.hal.HalMediaType;
import org.multibit.mbm.api.request.cart.purchaseorder.AdminUpdatePurchaseOrderRequest;
import org.multibit.mbm.api.request.cart.purchaseorder.BuyerPurchaseOrderItem;
import org.multibit.mbm.api.response.hal.purchaseorder.AdminPurchaseOrderBridge;
import org.multibit.mbm.api.response.hal.purchaseorder.AdminPurchaseOrderCollectionBridge;
import org.multibit.mbm.auth.Authority;
import org.multibit.mbm.auth.annotation.RestrictedTo;
import org.multibit.mbm.db.dao.ItemDao;
import org.multibit.mbm.db.dao.PurchaseOrderDao;
import org.multibit.mbm.core.model.Item;
import org.multibit.mbm.core.model.PurchaseOrder;
import org.multibit.mbm.core.model.User;
import org.multibit.mbm.resources.BaseResource;
import org.multibit.mbm.resources.ResourceAsserts;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Resource to provide the following to application:</p>
 * <ul>
 * <li>Provision of REST endpoints to manage CRUD operations by an administrator against a collection of {@link org.multibit.mbm.core.model.PurchaseOrder} entities</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Component
@Path("/admin/purchase-orders")
@Produces({HalMediaType.APPLICATION_HAL_JSON, HalMediaType.APPLICATION_HAL_XML})
public class AdminPurchaseOrderResource extends BaseResource {

  @Resource(name = "hibernatePurchaseOrderDao")
  PurchaseOrderDao purchaseOrderDao;

  @Resource(name = "hibernateItemDao")
  ItemDao itemDao;

  /**
   * Provide a paged response of all PurchaseOrders in the system
   *
   * @param buyerUser     A User with administrator rights
   * @param rawPageSize   The unvalidated page size
   * @param rawPageNumber The unvalidated page number
   *
   * @return A response containing a paged list of all PurchaseOrders
   */
  @GET
  @Timed
  @CacheControl(maxAge = 6, maxAgeUnit = TimeUnit.HOURS)
  public Response retrieveAllByPage(
    @RestrictedTo({Authority.ROLE_BUYER})
    User buyerUser,
    @QueryParam("ps") Optional<String> rawPageSize,
    @QueryParam("pn") Optional<String> rawPageNumber) {

    // Validation
    int pageSize = Integer.valueOf(rawPageSize.get());
    int pageNumber = Integer.valueOf(rawPageNumber.get());

    List<PurchaseOrder> purchaseOrders = purchaseOrderDao.getAllByPage(pageSize, pageNumber);

    // Provide a representation to the client
    AdminPurchaseOrderCollectionBridge bridge = new AdminPurchaseOrderCollectionBridge(uriInfo, Optional.of(buyerUser));

    return ok(bridge, purchaseOrders);

  }

  /**
   * Update an existing PurchaseOrder with the populated fields
   *
   * @param adminUser A purchaseOrder with administrator rights
   *
   * @return A response containing the full details of the updated entity
   */
  @PUT
  @Timed
  @Path("/{purchaseOrderId}")
  public Response update(
    @RestrictedTo({Authority.ROLE_BUYER})
    User adminUser,
    @PathParam("purchaseOrderId") Long purchaseOrderId,
    AdminUpdatePurchaseOrderRequest updatePurchaseOrderRequest) {

    // Retrieve the purchaseOrder
    Optional<PurchaseOrder> purchaseOrder = purchaseOrderDao.getById(purchaseOrderId);
    ResourceAsserts.assertPresent(purchaseOrder,"purchaseOrder");

    // Verify and apply any changes to the PurchaseOrder
    PurchaseOrder persistentPurchaseOrder = purchaseOrder.get();
    apply(updatePurchaseOrderRequest,persistentPurchaseOrder);

    // Persist the updated purchaseOrder
    persistentPurchaseOrder = purchaseOrderDao.saveOrUpdate(persistentPurchaseOrder);

    // Provide a representation to the client
    AdminPurchaseOrderBridge bridge = new AdminPurchaseOrderBridge(uriInfo, Optional.of(adminUser));

    return ok(bridge, persistentPurchaseOrder);

  }

  /**
   * TODO Refactor into a common handler
   * @param updateRequest The update request containing the changes
   * @param entity        The entity to which these changes will be applied
   */
  private void apply(AdminUpdatePurchaseOrderRequest updateRequest, PurchaseOrder entity) {

    for (BuyerPurchaseOrderItem supplierPurchaseOrderItem : updateRequest.getPurchaseOrderItems()) {
      ResourceAsserts.assertNotNull(supplierPurchaseOrderItem.getSKU(), "id");
      ResourceAsserts.assertPositive(supplierPurchaseOrderItem.getQuantity(), "quantity");

      Optional<Item> item = itemDao.getBySKU(supplierPurchaseOrderItem.getSKU());
      ResourceAsserts.assertPresent(item,"item");

      entity.setItemQuantity(item.get(),supplierPurchaseOrderItem.getQuantity());
    }
  }

  public void setPurchaseOrderDao(PurchaseOrderDao purchaseOrderDao) {
    this.purchaseOrderDao = purchaseOrderDao;
  }

  public void setItemDao(ItemDao itemDao) {
    this.itemDao = itemDao;
  }
}