package de.metas.vertical.pharma.msv3.protocol.order;

import de.metas.vertical.pharma.msv3.protocol.types.Id;
import de.metas.vertical.pharma.msv3.protocol.types.PZN;
import de.metas.vertical.pharma.msv3.protocol.types.Quantity;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-pharma.msv3.commons
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
public class OrderCreateRequestPackageItem
{
	Id id;
	PZN pzn;
	Quantity qty;
	DeliverySpecifications deliverySpecifications;

	@Builder
	private OrderCreateRequestPackageItem(
			@NonNull final PZN pzn,
			@NonNull final Quantity qty,
			@NonNull final DeliverySpecifications deliverySpecifications)
	{
		this.id = Id.random();
		this.pzn = pzn;
		this.qty = qty;
		this.deliverySpecifications = deliverySpecifications;
	}
}