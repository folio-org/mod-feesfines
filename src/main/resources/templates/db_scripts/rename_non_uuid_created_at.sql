UPDATE ${myuniversity}_${mymodule}.feefineactions
SET jsonb = jsonb - 'createdAt' || jsonb_build_object('originalCreatedAt', jsonb->'createdAt')
WHERE jsonb->'createdAt' IS NOT NULL
AND jsonb->>'createdAt' !~ '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}';