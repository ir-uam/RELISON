/*
 * Copyright (C) 2026 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io;

import java.util.ArrayList;
import java.util.List;

/** Utilities for parsing a single delimited row with CSV-style quoted fields. */
public final class DelimitedRow
{
    private DelimitedRow() { }

    /** Splits a row, honoring double-quoted fields and doubled quote escapes. */
    public static String[] parse(String line, String delimiter)
    {
        if (delimiter.length() != 1) return line.split(delimiter, -1);
        char separator = delimiter.charAt(0);
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (c == '"')
            {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"')
                {
                    field.append('"');
                    i++;
                }
                else quoted = !quoted;
            }
            else if (c == separator && !quoted)
            {
                fields.add(field.toString());
                field.setLength(0);
            }
            else field.append(c);
        }
        fields.add(field.toString());
        if (!fields.isEmpty() && fields.get(0).startsWith("\uFEFF"))
            fields.set(0, fields.get(0).substring(1));
        return fields.toArray(new String[0]);
    }
}
