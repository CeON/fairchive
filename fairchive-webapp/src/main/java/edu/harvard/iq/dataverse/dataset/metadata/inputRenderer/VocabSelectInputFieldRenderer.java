package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import java.util.Map;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns = true;

    /**
     * Sort vocabulary values list in localized labels order 
     */
    private boolean sortByLocalisedStringsOrder = false;


    private final ConditionalRendering conditionalRendering;


    // -------------------- CONSTRUCTORS --------------------

    public VocabSelectInputFieldRenderer(
            final boolean renderInTwoColumns,
            final boolean sortByLocalisedStringsOrder,
            final ConditionalRendering conditionalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.sortByLocalisedStringsOrder = sortByLocalisedStringsOrder;
        this.conditionalRendering = conditionalRendering;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns value provided in constructor
     */
    @Override
    public boolean renderInTwoColumns() {
        return this.renderInTwoColumns;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public Option<ConditionalRendering> getConditionalRendering() {
        return Option.of(this.conditionalRendering);
    }

    public boolean isSortByLocalisedStringsOrder() {
        return this.sortByLocalisedStringsOrder;
    }


    // -------------------- LOGIC --------------------

    public void processValueChange(final DatasetField field, 
    		final Map<DatasetFieldType, InputFieldRenderer> renderersByFieldType) {
    	
    	field.streamSiblings().forEach(sibling -> {
             final InputFieldRenderer renderer = renderersByFieldType.get(sibling.getDatasetFieldType());
             if (renderer != null && renderer.getConditionalRendering().isDefined()) {
                 sibling.clearValue();
                 sibling.setValidationMessage(null);
             }
         });
    }

    public boolean hasChangeListener(final DatasetField field, 
    		final Map<DatasetFieldType, InputFieldRenderer> renderersByFieldType) {
    	
        return field.streamSiblings()
        	.map(sibling -> renderersByFieldType.get(sibling.getDatasetFieldType()))
        	.anyMatch(renderer -> hasConditionalRenderingFor(field.getTypeName(), renderer));
    }

    private boolean hasConditionalRenderingFor(final String fieldTypeName, 
    		final InputFieldRenderer renderer) {
    	
        return Option.of(renderer)
                .flatMap(InputFieldRenderer::getConditionalRendering)
                .map(cr -> cr.getDatasetFieldName().equals(fieldTypeName))
                .getOrElse(false);
    }
}
