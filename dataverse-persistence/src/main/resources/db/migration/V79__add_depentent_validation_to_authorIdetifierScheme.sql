update datasetfieldtype set validation = '[{"name":"standard_input"}, {"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "authorIdentifier"}}]'
where id  = 11 and name = 'authorIdentifierScheme';
update datasetfieldtype set validation = '[{"name":"orcid_validator","parameters":{"context":["DATASET"], "authorIdentifierScheme": "ORCID"}}, {"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "authorIdentifierScheme"}}]'
where id  = 12 and name = 'authorIdentifier';