DROP VIEW IF EXISTS M_AttributeSetInstance_ID_AttributeInstances;

CREATE OR REPLACE VIEW M_AttributeSetInstance_ID_AttributeInstances AS
SELECT M_AttributeSetInstance_ID,
array_agg(M_Attribute_ID || '_' || value) as AttributeInstances
FROM
	(
		SELECT
			ai.M_AttributeSetInstance_ID, ai.M_Attribute_ID,
			CASE
			WHEN a.AttributeValueType = 'N'
				THEN  coalesce(ai.m_attributevalue_id :: character varying, ai.valuenumber :: character varying, '')
			WHEN a.AttributeValueType = 'D'
				THEN coalesce(ai.m_attributevalue_id :: character varying, ai.valuedate :: character varying, '')
			ELSE
				coalesce(ai.m_attributevalue_id :: character varying, ai.value, '')
			END AS Value
		FROM M_AttributeInstance ai
			JOIN M_Attribute a ON a.M_Attribute_ID = ai.M_Attribute_ID
	) as data
GROUP BY M_AttributeSetInstance_ID;
COMMENT ON VIEW M_AttributeSetInstance_ID_AttributeInstances
IS 'Returns M_AttributeSetInstance_IDs with arrays that represent all M_AttributeInstances of the respective ASI.
Each array element is a string containing of <M_Attribute_ID>_<Attribute-Instance-Value>';

CREATE OR REPLACE VIEW Report.fresh_Attributes AS 
SELECT * FROM
(
	SELECT
		CASE
			WHEN a.Value = '1000015' AND av.value = '01' THEN NULL -- ADR & Keine/Leer
			WHEN a.Value = '1000015' AND (av.value IS NOT NULL AND av.value != '') THEN 'AdR' -- ADR
			WHEN a.Value = '1000001' AND (av.value IS NOT NULL AND av.value != '') THEN av.value -- Herkunft
			WHEN a.Value = '1000021' AND (ai.value IS NOT NULL AND ai.value != '') THEN ai.Value -- MHD
			WHEN a.Value = 'HU_BestBeforeDate' AND (ai.valuedate IS NOT NULL) THEN 'MHD: '|| to_char(ai.valuedate, 'DD.MM.YYYY') --Best Before Date
			WHEN a.attributevaluetype = 'S' AND COALESCE(TRIM(ai.value),'') != '' THEN ai.value
			WHEN a.attributevaluetype = 'N' AND ai.valuenumber IS NOT NULL AND ai.valuenumber > 0 THEN ai.valuenumber::bpchar
			WHEN a.Value = 'M_Material_Tracking_ID' 
				THEN (SELECT mt.lot FROM m_material_tracking mt 
					WHERE mt.m_material_tracking_id = ai.value::numeric  )
			ELSE av.Name -- default 
		END AS ai_Value, 
		M_AttributeSetInstance_ID,
		a.Value as at_Value,
		a.Name as at_Name,
		a.IsAttrDocumentRelevant as at_IsAttrDocumentRelevant -- task
	FROM M_AttributeInstance ai
		LEFT OUTER JOIN M_Attributevalue av ON ai.M_Attributevalue_ID = av.M_Attributevalue_ID AND av.isActive = 'Y'
		LEFT OUTER JOIN M_Attribute a ON ai.M_Attribute_ID = a.M_Attribute_ID AND a.isActive = 'Y'
	WHERE
		/**
		 * 08014 - There are/were orderlines, that had M_AttributeSetInstance_ID = 0 when they were intended to not have
		 * Attributes set. Unfortunately there actually was an attribute set with ID = 0 which also had values set thus
		 * The report displayed attribute values even though it should not display them. The Attribute with the ID = 0
		 * Is invalid and therefore not returned by this view. That way, the Report will display nothing for ASI ID = 0
		 */ 
		ai.M_AttributeSetInstance_ID != 0
		AND ai.isActive = 'Y'
) att
WHERE COALESCE( ai_value, '') != ''
;
COMMENT ON VIEW Report.fresh_Attributes IS 'retrieves Attributes in the way they are needed for the jasper reports';
