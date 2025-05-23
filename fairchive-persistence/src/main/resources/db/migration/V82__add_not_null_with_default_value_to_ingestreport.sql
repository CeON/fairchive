ALTER TABLE ingestreport 
ALTER COLUMN errorkey SET DEFAULT 'UNKNOWN_ERROR';

UPDATE ingestreport
SET errorkey = 'UNKNOWN_ERROR'
WHERE errorkey IS NULL;

ALTER TABLE ingestreport
ALTER COLUMN errorkey SET NOT NULL;