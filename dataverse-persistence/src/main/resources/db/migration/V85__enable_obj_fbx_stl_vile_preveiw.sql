alter table public.externaltool add column fileextention text;

INSERT INTO public.externaltool
(description, displayname, toolparameters, toolurl, "type", contenttype, fileextention)
VALUES('Preview 3D model', 'Preview 3D model', '{"queryParameters":[{"fileUrl64":"{fileUrl64}"}]}', '{siteUrl}/3Dviewer.xhtml', 'PREVIEW', 'text/plain', 'obj');

INSERT INTO public.externaltool
(description, displayname, toolparameters, toolurl, "type", contenttype, fileextention)
VALUES('Preview 3D model', 'Preview 3D model', '{"queryParameters":[{"fileUrl64":"{fileUrl64}"}]}', '{siteUrl}/3Dviewer.xhtml', 'PREVIEW', 'application/octet-stream', 'fbx');

INSERT INTO public.externaltool
(description, displayname, toolparameters, toolurl, "type", contenttype, fileextention)
VALUES('Preview 3D model', 'Preview 3D model', '{"queryParameters":[{"fileUrl64":"{fileUrl64}"}]}', '{siteUrl}/3Dviewer.xhtml', 'PREVIEW', 'application/sla', 'stl');

INSERT INTO public.externaltool 
(description,displayname,toolparameters,toolurl,"type",contenttype,fileextention) 
VALUES('Preview 3D model', 'Preview 3D model', '{"queryParameters":[{"fileUrl64":"{fileUrl64}"}]}', '{siteUrl}/3Dviewer.xhtml', 'PREVIEW', 'application/octet-stream', '3ds');
