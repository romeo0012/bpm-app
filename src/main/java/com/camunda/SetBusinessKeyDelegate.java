package com.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public class SetBusinessKeyDelegate implements org.camunda.bpm.engine.delegate.JavaDelegate {
    public void execute(DelegateExecution execution) {
        try {
            Object processInstance = execution.getProcessInstance();
            
            // Použij reflection pro nastavení business key
            java.lang.reflect.Method setBusinessKey = processInstance.getClass()
                .getMethod("setBusinessKey", String.class);
            
            String businessKey = execution.getVariable("bk") != null 
                ? execution.getVariable("bk").toString() 
                : "";
            
            setBusinessKey.invoke(processInstance, businessKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set business key", e);
        }
    }
}
