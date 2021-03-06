package me.yingrui.segment.web.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import me.yingrui.segment.web.Application;
import me.yingrui.segment.web.UsingFixtures;
import me.yingrui.segment.web.api.model.Concept;
import me.yingrui.segment.web.api.model.PartOfSpeech;
import me.yingrui.segment.web.api.model.dto.ConceptDto;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class})
public class ConceptServiceTest extends UsingFixtures {

    @Autowired
    private ConceptService conceptService;

    @Test
    public void should_add_concept_in_database() {
        String c = uniq("Concept");

        Concept concept = addConcept(c, posN, null);

        Concept actualConcept = conceptService.getById(concept.getId());
        assertNotNull(actualConcept);
        assertEquals("N", actualConcept.getPartOfSpeech().getName());
        assertEquals(c, actualConcept.getName());
    }

    @Test
    public void should_add_parent_concept() {
        String c1 = uniq("Concept1");
        String c2 = uniq("Concept2");
        String c3 = uniq("Concept3");

        Concept concept1 = addConcept(c1, posN, null);
        Concept concept2 = addConcept(c2, posT, concept1);
        Concept concept3 = addConcept(c3, posT, concept2);

        Concept actualConcept = conceptService.getById(concept3.getId());
        assertNotNull(actualConcept);
        assertEquals("N", actualConcept.getParent().getParent().getPartOfSpeech().getName());
        assertEquals("T", actualConcept.getParent().getPartOfSpeech().getName());
        assertEquals(c1, actualConcept.getParent().getParent().getName());
        assertEquals(c2, actualConcept.getParent().getName());
        assertEquals(c3, actualConcept.getName());
    }

    @Test
    public void should_return_all_concepts_order_by_id_asc() {
        String c1 = uniq("Concept");
        String c2 = uniq("Concept");
        String c3 = uniq("Concept");

        Concept concept1 = addConcept(c1, posN, null);
        Concept concept2 = addConcept(c2, posT, concept1);
        Concept concept3 = addConcept(c3, posT, concept2);

        List<Concept> conceptList = conceptService.list();
        assertEquals(c1, conceptList.get(conceptList.size() - 3).getName());
        assertEquals(c2, conceptList.get(conceptList.size() - 2).getName());
        assertEquals(c3, conceptList.get(conceptList.size() - 1).getName());

        Concept grandChild = conceptList.get(conceptList.size() - 1);
        assertNotNull(grandChild);
        assertEquals("N", grandChild.getParent().getParent().getPartOfSpeech().getName());
        assertEquals("T", grandChild.getParent().getPartOfSpeech().getName());
        assertEquals(c1, grandChild.getParent().getParent().getName());
        assertEquals(c2, grandChild.getParent().getName());
        assertEquals(c3, grandChild.getName());
    }

    @Test
    public void should_build_concept_tree() {
        Concept concept1 = addConcept(uniq("Concept"), posN, null);
        Concept concept2 = addConcept(uniq("Concept"), posT, concept1);
        Concept concept3 = addConcept(uniq("Concept"), posT, concept2);

        ConceptDto root = conceptService.getConceptTree();
        assertNotNull(root);
        assertEquals("root", root.name);

        for (ConceptDto concept: root.children) {
            if(concept.name.equals(concept1.getName())) {
                assertEquals(concept1.getName(), concept.name);
                assertEquals(concept2.getName(), concept.children.get(0).name);
                assertEquals(1, concept.children.size());
                assertEquals(concept3.getName(), concept.children.get(0).children.get(0).name);
                assertEquals(1, concept.children.get(0).children.size());
                return;
            }
        }
        fail();
    }

    @Test
    public void should_return_children_of_concept() {
        String c1 = uniq("Concept");
        String c2 = uniq("Concept");
        String c3 = uniq("Concept");

        Concept concept1 = addConcept(c1, posN, null);
        Concept concept2 = addConcept(c2, posT, concept1);
        Concept concept3 = addConcept(c3, posT, concept1);

        List<Concept> children = conceptService.getChildren(concept1.getId());
        assertEquals(2, children.size());
        assertEquals(c2, children.get(0).getName());
        assertEquals(c3, children.get(1).getName());
    }

    private Concept addConcept(String c, PartOfSpeech pos, Concept parent) {
        Concept concept = new Concept();
        concept.setName(c);
        concept.setNote(uniq("Note"));
        concept.setPartOfSpeech(pos);
        concept.setParent(parent);
        conceptService.add(concept);
        return concept;
    }
}
