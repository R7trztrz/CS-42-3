package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Symbolically validates FR38 determinism, acyclicity, reachability and termination. */
@Component
public class QuestionnaireFlowValidator {

    public void validate(List<FlowItem> items) {
        if (items.isEmpty()) {
            return;
        }

        List<Set<Integer>> edges = buildEdges(items);
        rejectCycles(items, edges);
        rejectUnreachableItems(items, edges);
    }

    private List<Set<Integer>> buildEdges(List<FlowItem> items) {
        int end = items.size();
        List<Set<Integer>> edges = new ArrayList<>(items.size());
        for (int position = 0; position < items.size(); position++) {
            FlowItem item = items.get(position);
            Set<Integer> successors = new HashSet<>();
            item.rules().stream().map(FlowRule::targetPosition).forEach(successors::add);
            if (hasDefaultEdge(item)) {
                successors.add(position + 1 < items.size() ? position + 1 : end);
            }
            if (successors.isEmpty()) {
                throw invalid(
                        item,
                        position,
                        null,
                        "NON_TERMINATING_ITEM",
                        "Item has no valid answer transition"
                );
            }
            edges.add(successors);
        }
        return edges;
    }

    private boolean hasDefaultEdge(FlowItem item) {
        Question question = item.question();
        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            long domainSize = question.getOptions().size();
            return !question.isRequired() || item.rules().size() < domainSize;
        }
        if (question.getType() == QuestionType.SCALE) {
            long domainSize = (long) question.getScaleMax() - (long) question.getScaleMin() + 1L;
            return !question.isRequired() || item.rules().size() < domainSize;
        }
        return true;
    }

    private void rejectCycles(List<FlowItem> items, List<Set<Integer>> edges) {
        int[] state = new int[items.size()];
        ArrayDeque<Integer> path = new ArrayDeque<>();
        for (int node = 0; node < items.size(); node++) {
            if (state[node] == 0) {
                detectCycle(node, items, edges, state, path);
            }
        }
    }

    private void detectCycle(
            int node,
            List<FlowItem> items,
            List<Set<Integer>> edges,
            int[] state,
            ArrayDeque<Integer> path
    ) {
        state[node] = 1;
        path.addLast(node);
        for (int successor : edges.get(node)) {
            if (successor == items.size()) {
                continue;
            }
            if (state[successor] == 1) {
                List<Integer> cycle = new ArrayList<>(path);
                cycle = cycle.subList(cycle.indexOf(successor), cycle.size());
                cycle = new ArrayList<>(cycle);
                cycle.add(successor);
                throw invalid(
                        items.get(node),
                        node,
                        null,
                        "BRANCH_CYCLE",
                        "Questionnaire flow contains a cycle: " + cycle
                );
            }
            if (state[successor] == 0) {
                detectCycle(successor, items, edges, state, path);
            }
        }
        path.removeLast();
        state[node] = 2;
    }

    private void rejectUnreachableItems(List<FlowItem> items, List<Set<Integer>> edges) {
        boolean[] reached = new boolean[items.size()];
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        reached[0] = true;
        pending.add(0);
        while (!pending.isEmpty()) {
            int node = pending.removeFirst();
            for (int successor : edges.get(node)) {
                if (successor < items.size() && !reached[successor]) {
                    reached[successor] = true;
                    pending.add(successor);
                }
            }
        }
        for (int position = 0; position < reached.length; position++) {
            if (!reached[position]) {
                throw invalid(
                        items.get(position),
                        position,
                        null,
                        "UNREACHABLE_ITEM",
                        "Questionnaire item is unreachable from the first item"
                );
            }
        }
    }

    private QuestionnaireValidationException invalid(
            FlowItem item,
            int itemIndex,
            Integer ruleIndex,
            String code,
            String message
    ) {
        SurveyErrorDetail detail = new SurveyErrorDetail(
                "items[" + itemIndex + "].branchRules",
                itemIndex,
                item.itemId(),
                ruleIndex,
                code,
                message
        );
        return new QuestionnaireValidationException(message, List.of(detail));
    }

    public record FlowItem(UUID itemId, Question question, List<FlowRule> rules) {
        public FlowItem {
            rules = List.copyOf(rules);
        }
    }

    public record FlowRule(
            UUID sourceOptionId,
            Integer sourceScaleValue,
            int targetPosition,
            int ruleIndex
    ) {}
}
