package com.adolfoeloy.swflab.swf.domain.workflow;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SwfWorkflowRepository extends JpaRepository<SwfWorkflow, Integer> {

    Optional<SwfWorkflow> findByWorkflowId(String workflowId);
}
