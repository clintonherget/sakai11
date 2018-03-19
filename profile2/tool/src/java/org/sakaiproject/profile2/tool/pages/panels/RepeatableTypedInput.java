package org.sakaiproject.profile2.tool.pages.panels;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.ProfileWallLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.UserProfile;
import org.sakaiproject.profile2.tool.components.ComponentVisualErrorBehaviour;
import org.sakaiproject.profile2.tool.components.ErrorLevelsFeedbackMessageFilter;
import org.sakaiproject.profile2.tool.components.FeedbackLabel;
import org.sakaiproject.profile2.tool.components.PhoneNumberValidator;
import org.sakaiproject.profile2.util.ProfileConstants;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;

import org.sakaiproject.profile2.model.TypeInputEntry;

import java.io.Serializable;

public class RepeatableTypedInput extends Panel {
    private List<String> types;
    private ListView<TypeInputEntry> view;
    private Form form;

    public RepeatableTypedInput(String id,
                                PropertyModel<UserProfile> model,
                                final List<String> types) {
        super(id, model);

        form = new Form("form");

        view = new ListView<TypeInputEntry>("item", (List<TypeInputEntry>)this.getDefaultModelObject()) {
            public void populateItem(final ListItem<TypeInputEntry> item) {
                final TypeInputEntry entry = (TypeInputEntry)item.getDefaultModelObject();

                DropDownChoice<String> choices = new DropDownChoice<>("choices", new PropertyModel(entry, "type"), types);

                if (choices.getModelObject() == null) {
                    choices.setModelObject(types.get(0));
                }

                choices.setNullValid(false);
                choices.setOutputMarkupId(true);

                item.add(choices);
                item.add(new TextField<>("value", new PropertyModel(entry, "value")).setOutputMarkupId(true));

                AjaxButton removeItem = new AjaxButton("removeItem", form) {
                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form form) {
                        List<TypeInputEntry> entries = ((List<TypeInputEntry>)RepeatableTypedInput.this.getDefaultModelObject());

                        ((List<TypeInputEntry>)RepeatableTypedInput.this.getDefaultModelObject()).remove(entry);
                        target.add(RepeatableTypedInput.this);
                    }
                };

                removeItem.setOutputMarkupId(true);
                item.add(removeItem);
            }
        };

        view.setReuseItems(false);
        view.setOutputMarkupId(true);

        form.add(view);
        this.add(form);
        this.setOutputMarkupId(true);
    }

    public void addEntry(String type, String value) {
        List<TypeInputEntry> entries = (List<TypeInputEntry>)this.getDefaultModelObject();
        entries.add(new TypeInputEntry(type, value));
    }

    public void finish() {
        List<TypeInputEntry> entries = (List<TypeInputEntry>)this.getDefaultModelObject();
        if (entries.isEmpty()) {
            // Ensure we have a blank at the end of our list.
            addEntry(null, "");
        }

        AjaxButton addAnother = new AjaxButton("addAnother", form) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                RepeatableTypedInput.this.addEntry(null, "");
                target.add(RepeatableTypedInput.this);
            }
        };

        form.add(addAnother);
    }
}
