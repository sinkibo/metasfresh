package de.metas.inventory;

import java.util.List;

import org.adempiere.util.ISingletonService;
import org.compiere.model.I_M_Inventory;
import org.compiere.model.I_M_InventoryLine;

import de.metas.quantity.Quantity;

/**
 * @author ad
 *
 */
public interface IInventoryBL extends ISingletonService
{
	/**
	 * Gets the charge we use for internal inventory charge (from a sysconfig).
	 * Used in quick input and automatically generated inventory lines.
	 * Never returns non-positive.
	 */
	int getDefaultInternalChargeId();

	void addDescription(I_M_Inventory inventory, String descriptionToAdd);

	void addDescription(I_M_InventoryLine inventoryLine, String descriptionToAdd);

	boolean isComplete(I_M_Inventory inventory);

	boolean isSOTrx(I_M_InventoryLine inventoryLine);

	boolean isInternalUseInventory(I_M_InventoryLine inventoryLine);

	/**
	 * Get Movement Qty (absolute value)
	 * <li>negative value means outgoing trx
	 * <li>positive value means incoming trx
	 *
	 * @return movement qty
	 */
	Quantity getMovementQty(I_M_InventoryLine inventoryLine);

	Quantity getMovementQtyInStockingUOM(I_M_InventoryLine inventoryLine);

	void assignToInventoryCounters(List<I_M_InventoryLine> inventoryLines, int numberOfCounters);

}