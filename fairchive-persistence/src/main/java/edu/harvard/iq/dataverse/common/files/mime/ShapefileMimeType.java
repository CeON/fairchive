package edu.harvard.iq.dataverse.common.files.mime;

public enum ShapefileMimeType implements MimeType {

	SHAPEFILE_FILE_TYPE("application/zipped-shapefile", "Shapefile as ZIP Archive");

	private final String mimeValue;
	private final String friendlyName;

	ShapefileMimeType(final String mimeType, final String friendlyName) {
		this.mimeValue = mimeType;
		this.friendlyName = friendlyName;
	}

	@Override
	public String getMimeValue() {
		return this.mimeValue;
	}

	public String getFriendlyName() {
		return this.friendlyName;
	}
}
