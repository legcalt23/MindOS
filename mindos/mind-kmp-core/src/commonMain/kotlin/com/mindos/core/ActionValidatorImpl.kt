package com.mindos.core

import com.mindos.contracts.ActionContractValidator
import com.mindos.contracts.DefaultActionContractValidator
import com.mindos.contracts.RouterActionDto
import com.mindos.contracts.ValidationReportDto

class ActionValidatorImpl(
    private val delegate: ActionContractValidator = DefaultActionContractValidator,
) : ActionContractValidator {
    override fun validate(action: RouterActionDto): ValidationReportDto =
        delegate.validate(action)
}
