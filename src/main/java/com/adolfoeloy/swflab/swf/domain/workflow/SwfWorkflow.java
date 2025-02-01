package com.adolfoeloy.swflab.swf.domain.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "swf_workflow")
public class SwfWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "swf_generator")
    @SequenceGenerator(name = "swf_generator", sequenceName = "swf_sq", initialValue = 1, allocationSize = 1)
    private int id;

    private String email;

    private String phone;

    @Column(name = "workflow_id")
    private String workflowId;

}
