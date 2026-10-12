import { NgTemplateOutlet } from "@angular/common";
import { Component, ContentChild, Input, TemplateRef } from "@angular/core";

@Component(
{
    selector: "app-query-state",
    standalone: true,
    imports: [NgTemplateOutlet],
    templateUrl: './query-state.html'
})
export class QueryState
{
    @Input({required: true}) loading: boolean = false;
    @Input({required: true}) error: boolean = false;
    @Input() empty: boolean = false;

    @ContentChild('loading') loadingTemplate?: TemplateRef<unknown>;
    @ContentChild('error') errorTemplate?: TemplateRef<unknown>;
    @ContentChild('empty') emptyTemplate?: TemplateRef<unknown>;
    @ContentChild('content') contentTemplate?: TemplateRef<unknown>;
}