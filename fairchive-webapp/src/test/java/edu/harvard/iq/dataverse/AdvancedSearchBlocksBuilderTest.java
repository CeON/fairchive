package edu.harvard.iq.dataverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.faces.model.SelectItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldFactory;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;

@ExtendWith(MockitoExtension.class)
public class AdvancedSearchBlocksBuilderTest {

    @InjectMocks
    private AdvancedSearchBlocksBuilder advancedSearchBlocksBuilder;

    @Mock
    private SearchFieldFactory searchFieldFactory;
    @Mock
    private DatasetFieldServiceBean datasetFieldService;
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    
    @Test
    void createDatasetMetadataBlocks() {
        // given
        Dataverse dataverse = new Dataverse();
        MetadataBlock block1 = MocksFactory.makeMetadataBlock("citation", "Citation Metadata", 0);
        MetadataBlock block2 = MocksFactory.makeMetadataBlock("block2", "Block Name2", 1);
        MetadataBlock block3 = MocksFactory.makeMetadataBlock("block3", "Block Name3", 2);
        dataverse.setMetadataBlocks(ImmutableList.of(block1, block2, block3));

        List<Long> blockIds = ImmutableList.of(block1.getId(), block2.getId(), block3.getId());

        DatasetFieldType field1 = MocksFactory.makeDatasetFieldType("field1", FieldType.TEXT, false, block1);
        DatasetFieldType field2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, block1);
        DatasetFieldType field3 = MocksFactory.makeDatasetFieldType("field3", FieldType.TEXT, false, block2);
        DatasetFieldType field4 = MocksFactory.makeDatasetFieldType("field4", FieldType.TEXT, false, block2);

        when(datasetFieldService.findAllAdvancedSearchFieldTypesByMetadataBlockIds(eq(blockIds))).thenReturn(
                ImmutableList.of(field1, field2, field3, field4));
        when(searchFieldFactory.create(any())).thenAnswer(invocation -> new TextSearchField(invocation.getArgument(0)));
            
        
        // when
        List<SearchBlock> searchBlocks = advancedSearchBlocksBuilder.createDatasetMetadataBlocks(dataverse);
        // then
        assertThat(searchBlocks).hasSize(3);
        assertThat(searchBlocks).extracting(SearchBlock::getBlockName).containsExactly("citation", "block2", "block3");
        assertThat(searchBlocks.get(0).getSearchFields())
            .extracting(SearchField::getName, SearchField::getSearchFieldType)
            .containsExactly(
                    tuple("field1", SearchFieldType.TEXT),
                    tuple("field2", SearchFieldType.TEXT),
                    tuple("dsPersistentId", SearchFieldType.TEXT),
                    tuple("dsPublicationDate", SearchFieldType.DATE));
        assertThat(searchBlocks.get(1).getSearchFields())
            .extracting(SearchField::getName, SearchField::getSearchFieldType)
            .containsExactly(
                    tuple("field3", SearchFieldType.TEXT),
                    tuple("field4", SearchFieldType.TEXT));
        assertThat(searchBlocks.get(2).getSearchFields()).isEmpty();
    }
    
    @Test
    void createDatasetMetadataBlocks__dataverse_is_not_metadata_block_root() {
        // given
        Dataverse rootDataverse = new Dataverse();
        MetadataBlock block1 = MocksFactory.makeMetadataBlock("citation", "Citation Metadata", 0);
        rootDataverse.setMetadataBlocks(ImmutableList.of(block1));
        rootDataverse.setMetadataBlockRoot(true);
        Dataverse dataverse = new Dataverse();
        dataverse.setOwner(rootDataverse);
        dataverse.setMetadataBlockRoot(false);

        List<Long> blockIds = ImmutableList.of(block1.getId());

        DatasetFieldType field1 = MocksFactory.makeDatasetFieldType("field1", FieldType.TEXT, false, block1);
        DatasetFieldType field2 = MocksFactory.makeDatasetFieldType("field2", FieldType.TEXT, false, block1);

        when(datasetFieldService.findAllAdvancedSearchFieldTypesByMetadataBlockIds(eq(blockIds))).thenReturn(
                ImmutableList.of(field1, field2));
        when(searchFieldFactory.create(any())).thenAnswer(invocation -> new TextSearchField(invocation.getArgument(0)));
            
        
        // when
        List<SearchBlock> searchBlocks = advancedSearchBlocksBuilder.createDatasetMetadataBlocks(rootDataverse);
        // then
        assertThat(searchBlocks).hasSize(1);
        assertThat(searchBlocks).extracting(SearchBlock::getBlockName).containsExactly("citation");
        assertThat(searchBlocks.get(0).getSearchFields())
            .extracting(SearchField::getName, SearchField::getSearchFieldType)
            .containsExactly(
                    tuple("field1", SearchFieldType.TEXT),
                    tuple("field2", SearchFieldType.TEXT),
                    tuple("dsPersistentId", SearchFieldType.TEXT),
                    tuple("dsPublicationDate", SearchFieldType.DATE));
    }

    @Test
    void createDataversesBlock() {
        // given
        MetadataBlock citationBlock = MocksFactory.makeMetadataBlock("citation", "Citation Metadata", 0);
        DatasetFieldType subjectField = MocksFactory.makeControlledVocabDatasetFieldType(
                DatasetFieldConstant.subject, true, citationBlock, "Chemistry", "Astronomy");
        when(datasetFieldService.findByName(DatasetFieldConstant.subject)).thenReturn(subjectField);

        // when
        SearchBlock dataversesBlock = advancedSearchBlocksBuilder.createDataversesBlock();

        // then
        assertThat(dataversesBlock).extracting(SearchBlock::getBlockName).isEqualTo("dataverses");
        assertThat(dataversesBlock.getSearchFields())
            .extracting(SearchField::getName, SearchField::getSearchFieldType)
            .containsExactly(
                    tuple("dvName", SearchFieldType.TEXT),
                    tuple("dvAlias", SearchFieldType.TEXT),
                    tuple("dvAffiliation", SearchFieldType.TEXT),
                    tuple("dvDescription", SearchFieldType.TEXT),
                    tuple("dvSubject", SearchFieldType.CHECKBOX));

        CheckboxSearchField subjectSearchField = dataversesBlock.getSearchFields().stream()
                .filter(sfield -> "dvSubject".equals(sfield.getName()))
                .map(sfield -> (CheckboxSearchField)sfield)
                .findFirst()
                .orElseThrow(RuntimeException::new);

        assertThat(subjectSearchField.getCheckboxLabelAndValue())
            .extracting(c -> c._1(), c -> c._2())
            .containsExactly(
                    tuple("Chemistry", "Chemistry"),
                    tuple("Astronomy", "Astronomy"));
    }

    @Test
    void createFilesBlock() {
        // given
        when(termsOfUseSelectItemsFactory.buildLicenseSelectItems()).thenReturn(ImmutableList.of(
                new SelectItem("LICENSE:1", "License 1"),
                new SelectItem("LICENSE:2", "License 2")));

        // when
        SearchBlock filesBlock = advancedSearchBlocksBuilder.createFilesBlock();

        // then
        assertThat(filesBlock).extracting(SearchBlock::getBlockName).isEqualTo("files");
        assertThat(filesBlock.getSearchFields())
            .extracting(SearchField::getName, SearchField::getSearchFieldType)
            .containsExactly(
                    tuple("fileName", SearchFieldType.TEXT),
                    tuple("fileDescription", SearchFieldType.TEXT),
                    tuple("fileExtension", SearchFieldType.TEXT),
                    tuple("filePersistentId", SearchFieldType.TEXT),
                    tuple("variableName", SearchFieldType.TEXT),
                    tuple("variableLabel", SearchFieldType.TEXT),
                    tuple("license", SearchFieldType.CHECKBOX));

        CheckboxSearchField licenseSearchField = filesBlock.getSearchFields().stream()
                .filter(sfield -> "license".equals(sfield.getName()))
                .map(sfield -> (CheckboxSearchField)sfield)
                .findFirst()
                .orElseThrow(RuntimeException::new);

        assertThat(licenseSearchField.getCheckboxLabelAndValue())
            .extracting(c -> c._1(), c -> c._2())
            .containsExactly(
                    tuple("License 1", "LICENSE:1"),
                    tuple("License 2", "LICENSE:2"));
    }
}
