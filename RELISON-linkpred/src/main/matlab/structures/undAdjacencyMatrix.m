%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function adj = undAdjacencyMatrix(index,train)
% UNDADJACENCYMATRIX Finds the undirected adjacency matrix of a graph
%   @args index the users
%   @args train the training data containing the links
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    adj = zeros(length(index), length(index));
    for i = 1:size(train,1)
        uIdx = find(index == train(i,1));
        vIdx = find(index == train(i,2));
        adj(uIdx,vIdx) = 1.0;
        adj(vIdx, uIdx) = 1.0;
    end

end