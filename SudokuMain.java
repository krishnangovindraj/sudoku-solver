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

import com.vaticle.typedb.client.api.TypeDBOptions;
import com.vaticle.typedb.client.api.TypeDBSession;
import com.vaticle.typedb.client.api.TypeDBTransaction;
import com.vaticle.typedb.client.connection.core.CoreClient;
import com.vaticle.typeql.lang.TypeQL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SudokuMain {

    public static void loadSchemaFromFile(String filePath, TypeDBTransaction tx) {
        try {
            System.out.println("Loading... " + filePath);
            System.out.println(System.getProperty("user.dir"));
            String s = Files.lines(Paths.get(filePath)).collect(Collectors.joining("\n"));
            tx.query().define(TypeQL.parseQuery(s).asDefine());
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void loadDataFromFile(String filePath, TypeDBTransaction tx) {
        try {
            System.out.println("Loading... " + filePath);
            String s = Files.lines(Paths.get(filePath)).collect(Collectors.joining("\n"));
            tx.query().insert(TypeQL.parseQuery(s).asInsert());
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static int[][] readSudoku(String filePath) throws IOException {
        int[] sudoku1d = Files.lines(Paths.get(filePath))
                .flatMap(line -> Stream.of(line.split(" "))).mapToInt(Integer::parseInt).toArray();
        int size = (int) Math.sqrt(sudoku1d.length);
        int [][] sudoku = new int[size][size];
        for(int i = 0; i < size ; i++){
            System.arraycopy(sudoku1d, i * size, sudoku[i], 0, size);
        }
        return sudoku;
    }

    public static void main(String ...args) throws IOException {
        String sudokuFilePath = args.length > 0? args[0] : "resources/sudoku";
        String sudokuSchemaPath = "resources/sudoku6x6_schema.tql";
        String sudokuDataPath = "resources/sudoku6x6_data.tql";
        String database = "sudoku6x6";

        try(CoreClient client = new CoreClient("localhost:1729")) {
            if (client.databases().contains(database)) client.databases().get(database).delete();
            client.databases().create(database);

            try(TypeDBSession session = client.session(database, TypeDBSession.Type.SCHEMA)) {
                try (TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.WRITE)) {
                    loadSchemaFromFile(sudokuSchemaPath, tx);
                    tx.commit();
                }
            }
            try(TypeDBSession session = client.session(database, TypeDBSession.Type.DATA)) {
                try (TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.WRITE)) {
                    loadDataFromFile(sudokuDataPath, tx);
                    tx.commit();
                }

                try (TypeDBTransaction tx = session.transaction(TypeDBTransaction.Type.READ, TypeDBOptions.core().infer(true))) {
                    int[][] sudoku = readSudoku(sudokuFilePath);
                    SudokuSolver solver = new SudokuSolver();
                    SudokuSolver.Solution solution = solver.solve(tx, sudoku);
                    solution.print();
                }
            }
        }
    }
}
