import grails.plugins.crm.notes.CrmNote
import org.codehaus.groovy.grails.commons.GrailsClassUtils

/*
* Copyright (c) 2012 Goran Ehrsson.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
class CrmNotesGrailsPlugin {
    def groupId = "grails.crm"
    def version = "1.2.0"
    def grailsVersion = "2.2 > *"
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/grails/plugins/crm/notes/TestEntity.groovy",
            "src/groovy/grails/plugins/crm/notes/TestSecurityDelegate.groovy"
    ]

    def title = "GR8 CRM Notes"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def organization = [name: "Technipelago AB", url: "http://www.technipelago.se/"]
    def description = "Let users add notes to domain instances in GR8 CRM"
    def documentation = "http://grails.org/plugin/crm-notes"
    def license = "APACHE"
    def issueManagement = [system: "github", url: "https://github.com/goeh/grails-crm-notes/issues"]
    def scm = [url: "https://github.com/goeh/grails-crm-notes"]

    def observe = ["domain"]

    def features = {
        crmNotes {
            description "Add notes to objects"
            permissions {
                guest "crmNotes:show"
                partner "crmNotes:*"
                user "crmNotes:*"
                admin "crmNotes:*"
            }
            hidden true
            statistics { tenant ->
                def total = CrmNote.countByTenantId(tenant)
                def updated = CrmNote.countByTenantIdAndLastUpdatedGreaterThan(tenant, new Date() - 31)
                def usage
                if (total > 0) {
                    def tmp = updated / total
                    if (tmp < 0.1) {
                        usage = 'low'
                    } else if (tmp < 0.3) {
                        usage = 'medium'
                    } else {
                        usage = 'high'
                    }
                } else {
                    usage = 'none'
                }
                return [usage: usage, objects: total]
            }
        }
    }

    def doWithDynamicMethods = { ctx ->
        def crmNotesService = ctx.getBean("crmNotesService")
        for (domainClass in application.domainClasses) {
            if (isNoteable(domainClass)) {
                addDomainMethods(domainClass.clazz.metaClass, crmNotesService)
            }
        }
    }

    def onChange = { event ->
        def ctx = event.ctx
        if (event.source && ctx && event.application) {
            def service = ctx.getBean('crmNotesService')
            // enhance domain classes with noteable property
            if ((event.source instanceof Class) && application.isDomainClass(event.source)) {
                def domainClass = application.getDomainClass(event.source.name)
                if (isNoteable(domainClass)) {
                    addDomainMethods(domainClass.metaClass, service)
                }
            }
        }
    }

    private void addDomainMethods(MetaClass mc, def crmNotesService) {
        mc.addNote = { String subject, String text, String author = null ->
            crmNotesService.create(delegate, subject, text, author, true)
        }
    }

    private boolean isNoteable(domainClass) {
        def val = GrailsClassUtils.getStaticPropertyValue(domainClass.clazz, "noteable")
        if (val != null) {
            return val
        }
        // If something is taggable, it's also noteable (unless specified different above)
        GrailsClassUtils.getStaticPropertyValue(domainClass.clazz, "taggable")
    }
}
