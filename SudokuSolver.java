/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.vaticle.typedb.example.sudoku;

import com.vaticle.typedb.client.api.TypeDBTransaction;
import com.vaticle.typedb.client.api.concept.Concept;
import com.vaticle.typedb.client.api.concept.thing.Relation;
import com.vaticle.typedb.client.api.concept.thing.Thing;
import com.vaticle.typeql.lang.TypeQL;
import com.vaticle.typeql.lang.pattern.Pattern;
import com.vaticle.typeql.lang.pattern.variable.ThingVariable;
import com.vaticle.typeql.lang.pattern.variable.UnboundVariable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SudokuSolver {

    static class Solution {
        private final Map<String, Long> mappings = new HashMap<>();
        private final int size;

        Solution(Relation.Remote solution) {
            this.size = (int) Math.sqrt(solution.getRelating().count());

            solution.getPlayersByRoleType().forEach((key, value) -> {
                Thing rp = value.iterator().next();
                mappings.put(key.getLabel().name(), rp.asAttribute().asLong().getValue());
            });
        }

        public void print() {
            System.out.println("Found solution:");
            for(int i = 1; i <= size ; i++){
                for(int j = 1; j <= size ; j++){
                    String role = "pos" + (i) + (j);
                    System.out.print(mappings.get(role) + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    public Pattern queryPattern(TypeDBTransaction tx, int[][] initial){
        List<Pattern> statements = new ArrayList<>();

        ThingVariable.Relation solutionRelation = null;
        int size = initial.length;
        UnboundVariable prevRp = null;
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                String role = "pos" + (i+1) + (j+1);
                int val = initial[i][j];
                UnboundVariable rp = TypeQL.var(role);
                if (solutionRelation == null) solutionRelation = TypeQL.var("r").rel(role, rp);
                else solutionRelation = solutionRelation.rel(role, rp);
                if (val != 0 ) {
                    statements.add(rp.eq(val));
                    // TODO: Remove: Hack for reasoner-planner to combine all equalities into a single retrievable
                    if (prevRp != null) statements.add(prevRp.neq(rp));
                    prevRp = rp;
                }
            }
        }

        solutionRelation.isa("solution");
        statements.add(solutionRelation);
        return TypeQL.and(statements);
    }

    public Solution solve(TypeDBTransaction tx, int[][] sudoku){
        printSudoku(sudoku);
        Pattern sudokuPattern = queryPattern(tx, sudoku);
        Concept solutionRelation = tx.query().match(TypeQL.match(sudokuPattern).limit(1))
                .map(ans -> ans.get("r"))
                .findFirst().orElse(null);
        return new Solution(solutionRelation.asRelation().asRemote(tx));
    }

    private void printSudoku(int[][] sudoku) {
        System.out.println("Solving the following Sudoku:");
        for (int[] ints : sudoku) {
            for (int j = 0; j < sudoku.length; j++) {
                int val = ints[j];
                System.out.print((val != 0 ? val : ".") + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}