package com.adolfoeloy.swflab.swf.domain.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SwfWorkflowRepository extends JpaRepository<SwfWorkflow, Integer> {

}
