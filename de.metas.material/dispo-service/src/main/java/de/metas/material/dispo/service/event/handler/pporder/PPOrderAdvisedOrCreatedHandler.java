package de.metas.material.dispo.service.event.handler.pporder;

import java.util.Optional;

import javax.annotation.Nullable;

import de.metas.material.dispo.commons.candidate.Candidate;
import de.metas.material.dispo.commons.candidate.Candidate.CandidateBuilder;
import de.metas.material.dispo.commons.candidate.CandidateBusinessCase;
import de.metas.material.dispo.commons.candidate.CandidateType;
import de.metas.material.dispo.commons.candidate.businesscase.DemandDetail;
import de.metas.material.dispo.commons.candidate.businesscase.Flag;
import de.metas.material.dispo.commons.candidate.businesscase.ProductionDetail;
import de.metas.material.dispo.commons.candidate.businesscase.ProductionDetail.ProductionDetailBuilder;
import de.metas.material.dispo.commons.repository.CandidateRepositoryRetrieval;
import de.metas.material.dispo.commons.repository.query.CandidatesQuery;
import de.metas.material.dispo.service.candidatechange.CandidateChangeService;
import de.metas.material.event.MaterialEventHandler;
import de.metas.material.event.commons.MaterialDescriptor;
import de.metas.material.event.commons.SupplyRequiredDescriptor;
import de.metas.material.event.pporder.AbstractPPOrderEvent;
import de.metas.material.event.pporder.MaterialDispoGroupId;
import de.metas.material.event.pporder.PPOrder;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-material-dispo
 * %%
 * Copyright (C) 2017 metas GmbH
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

abstract class PPOrderAdvisedOrCreatedHandler<T extends AbstractPPOrderEvent> implements MaterialEventHandler<T>
{
	private final CandidateChangeService candidateChangeService;
	private final CandidateRepositoryRetrieval candidateRepositoryRetrieval;

	/**
	 *
	 * @param candidateChangeService
	 * @param candidateService needed in case we directly request a {@link PpOrderSuggestedEvent}'s proposed PP_Order to be created.
	 */
	PPOrderAdvisedOrCreatedHandler(
			@NonNull final CandidateChangeService candidateChangeService,
			@NonNull final CandidateRepositoryRetrieval candidateRepositoryRetrieval)
	{
		this.candidateChangeService = candidateChangeService;
		this.candidateRepositoryRetrieval = candidateRepositoryRetrieval;
	}

	/**
	 * @return candidateGroupId
	 */
	protected final MaterialDispoGroupId handleAbstractPPOrderEvent(@NonNull final AbstractPPOrderEvent ppOrderEvent)
	{
		final Candidate headerCandidate = createHeaderCandidate(ppOrderEvent);

		// NOTE: candidates for PPOrderLines will be created when the manufacturing order is completed

		return headerCandidate.getGroupId();
	}

	private Candidate createHeaderCandidate(@NonNull final AbstractPPOrderEvent ppOrderEvent)
	{
		final PPOrder ppOrder = ppOrderEvent.getPpOrder();
		final SupplyRequiredDescriptor supplyRequiredDescriptor = ppOrderEvent.getSupplyRequiredDescriptor();

		final CandidatesQuery preExistingSupplyQuery = createPreExistingCandidatesQuery(ppOrder, supplyRequiredDescriptor);
		final Candidate existingCandidateOrNull = candidateRepositoryRetrieval.retrieveLatestMatchOrNull(preExistingSupplyQuery);

		final CandidateBuilder builder = existingCandidateOrNull != null
				? existingCandidateOrNull.toBuilder()
				: Candidate.builderForClientAndOrgId(ppOrder.getClientAndOrgId());

		final ProductionDetail headerCandidateProductionDetail = createProductionDetailForPPOrder(
				ppOrderEvent,
				existingCandidateOrNull);
		final MaterialDescriptor headerCandidateMaterialDescriptor = createMaterialDescriptorForPPOrder(ppOrder);
		final DemandDetail headerCandidateDemandDetail = PPOrderHandlerUtils.computeDemandDetailOrNull(
				CandidateType.SUPPLY,
				supplyRequiredDescriptor,
				headerCandidateMaterialDescriptor);

		final Candidate headerCandidate = builder
				.type(CandidateType.SUPPLY)
				.businessCase(CandidateBusinessCase.PRODUCTION)
				// .status(candidateStatus)
				.businessCaseDetail(headerCandidateProductionDetail)
				.additionalDemandDetail(headerCandidateDemandDetail)
				.materialDescriptor(headerCandidateMaterialDescriptor)
				// .groupId(null) // will be set after save
				.build();

		final Candidate headerCandidateWithGroupId = candidateChangeService.onCandidateNewOrChange(headerCandidate);
		return headerCandidateWithGroupId;
	}

	protected abstract CandidatesQuery createPreExistingCandidatesQuery(
			PPOrder ppOrder,
			@Nullable SupplyRequiredDescriptor supplyRequiredDescriptor);

	private static MaterialDescriptor createMaterialDescriptorForPPOrder(final PPOrder ppOrder)
	{
		return MaterialDescriptor.builder()
				.date(ppOrder.getDatePromised())
				.productDescriptor(ppOrder.getProductDescriptor())
				.quantity(ppOrder.getQtyOpen())
				.warehouseId(ppOrder.getWarehouseId())
				// .customerId(ppOrder.getBPartnerId()) not 100% sure if the ppOrder's bPartner is the customer this is made for
				.build();
	}

	private ProductionDetail createProductionDetailForPPOrder(
			@NonNull final AbstractPPOrderEvent ppOrderEvent,
			@Nullable final Candidate existingCandidateOrNull)
	{
		final PPOrder ppOrder = ppOrderEvent.getPpOrder();

		return prepareProductionDetail(existingCandidateOrNull)
				.advised(extractIsAdviseEvent(ppOrderEvent))
				.pickDirectlyIfFeasible(extractIsDirectlyPickSupply(ppOrderEvent))
				.qty(ppOrder.getQtyRequired())
				.plantId(ppOrder.getPlantId())
				.productPlanningId(ppOrder.getProductPlanningId())
				.ppOrderId(ppOrder.getPpOrderId())
				.ppOrderDocStatus(ppOrder.getDocStatus())
				.build();
	}

	/**
	 * @return initial builder with existing values, or create a new one
	 */
	private static ProductionDetailBuilder prepareProductionDetail(@Nullable final Candidate existingCandidateOrNull)
	{
		return Optional.ofNullable(existingCandidateOrNull)
				.map(Candidate::getBusinessCaseDetail)
				.map(ProductionDetail::cast)
				.map(ProductionDetail::toBuilder)
				.orElse(ProductionDetail.builder());
	}

	protected abstract Flag extractIsAdviseEvent(@NonNull final AbstractPPOrderEvent ppOrderEvent);

	protected abstract Flag extractIsDirectlyPickSupply(@NonNull final AbstractPPOrderEvent ppOrderEvent);
}
