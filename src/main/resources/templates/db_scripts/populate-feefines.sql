INSERT INTO feefines (id, jsonb) VALUES
('9523cb96-e752-40c2-89da-60f3961a488d',
'{
 "id": "9523cb96-e752-40c2-89da-60f3961a488d",
 "feeFineType": "Overdue fine",
 "automatic": true
}'),
('d20df2fb-45fd-4184-b238-0d25747ffdd9',
'{
 "id": "d20df2fb-45fd-4184-b238-0d25747ffdd9",
 "feeFineType": "Replacement processing fee",
 "automatic": true
}'),
('cf238f9f-7018-47b7-b815-bb2db798e19f',
'{
 "id": "cf238f9f-7018-47b7-b815-bb2db798e19f",
 "feeFineType": "Lost item fee",
 "automatic": true
}'),
('c7dede15-aa48-45ed-860b-f996540180e0',
'{
 "id": "c7dede15-aa48-45ed-860b-f996540180e0",
 "feeFineType": "Lost item processing fee",
 "automatic": true
}') ON CONFLICT DO NOTHING;

INSERT INTO ${myuniversity}_${mymodule}.feefines (id, jsonb) VALUES
( '73785370-d3bd-4d92-942d-ae2268e02ded',
  '{
   "id": "73785370-d3bd-4d92-942d-ae2268e02ded",
   "feeFineType": "Lost item fee (actual cost)",
   "automatic": true
  }'
) ON CONFLICT DO NOTHING;
