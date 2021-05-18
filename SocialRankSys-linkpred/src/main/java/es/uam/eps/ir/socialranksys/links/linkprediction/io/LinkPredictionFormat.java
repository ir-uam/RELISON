/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.io;

import es.uam.eps.ir.socialranksys.links.linkprediction.Prediction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Recommendation writers and readers with a common format.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface LinkPredictionFormat<U>
{

    /**
     * Gets a writer for a file path.
     *
     * @param path file path
     * @return a link prediction writer
     * @throws IOException if path does not exist or IO error
     */
    default Writer<U> getWriter(Path path) throws IOException
    {
        return getWriter(Files.newOutputStream(path));
    }

    /**
     * Gets a writer for a file path.
     *
     * @param path file path
     * @return a link prediction writer
     * @throws IOException if path does not exist or IO error
     */
    default Writer<U> getWriter(String path) throws IOException {
        return getWriter(new File(path));
    }

    /**
     * Gets a writer for a file.
     *
     * @param file file
     * @return a link prediction writer
     * @throws IOException if path does not exist or IO error
     */
    default Writer<U> getWriter(File file) throws IOException {
        return getWriter(new FileOutputStream(file));
    }

    /**
     * Gets a writer for an output stream.
     *
     * @param out output stream
     * @return a link prediction writer
     * @throws IOException if path does not exist or IO error
     */
    Writer<U> getWriter(OutputStream out) throws IOException;

    /**
     * Link prediction writer.
     *
     * @param <U> type of the users
     */
    interface Writer<U> extends Closeable, Consumer<Prediction<U>> {

        /**
         * Writes the prediction.
         *
         * @param prediction to be written
         * @throws IOException when IO error
         */
        void write(Prediction<U> prediction) throws IOException;

        @Override
        default void accept(Prediction<U> prediction) {
            try {
                write(prediction);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

    }

    /**
     * Gets a reader for a file path.
     *
     * @param path file path
     * @return a link prediction reader
     * @throws IOException when IO error
     */
    default Reader<U> getReader(Path path) throws IOException {
        return getReader(Files.newInputStream(path));
    }

    /**
     * Gets a reader for a file path.
     *
     * @param path file path
     * @return a link prediction reader
     * @throws IOException when IO error
     */
    default Reader<U> getReader(String path) throws IOException {
        return getReader(new File(path));
    }

    /**
     * Gets a reader for a file.
     *
     * @param file file
     * @return a link prediction reader
     * @throws IOException when IO error
     */
    default Reader<U> getReader(File file) throws IOException {
        return getReader(new FileInputStream(file));
    }

    /**
     * Gets a reader for an input stream.
     *
     * @param in input stream
     * @return a recommendation reader
     * @throws IOException when IO error
     */
    Reader<U> getReader(InputStream in) throws IOException;

    /**
     * Recommendation reader.
     *
     * @param <U> type of the users
     */
    interface Reader<U> extends Supplier<Prediction<U>>
    {
        /**
         * Reads the prediction
         *
         * @return a stream of recommendations
         */
        Prediction<U> read();

        @Override
        default Prediction<U> get() {
            return read();
        }

    }
}
