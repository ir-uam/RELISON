%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%

% First, we define the user index, the training network and the output directory
indexroute = 'user index';
trainroute = 'training graph';
outputdir = 'output directory';

% Then, we import the index and the training network
index = importdata(indexroute);
train = importdata(trainroute);

% We find the adjacency matrix (and the undirected version)
graph = adjacencyMatrix(index,train);
ugraph = undAdjacencyMatrix(index,train);

% Find the number of users and edges
numUsers = length(index);
numEdges = sum(sum(graph));
numUEdges = sum(sum(ugraph));
T = zeros(numUsers, numUsers);

% Hitting and commute time
alpha = 0.9;
for i = 1:numUsers
    rowsum = sum(graph(i,:));
    if rowsum == 0
        for j=1:numUsers
            T(i,j) = 1.0/numUsers;
        end
    else
        for j=1:numUsers
            T(i,j) = alpha./numUsers + graph(i,j).*(1-alpha)./rowsum;
        end
    end
end

ht = -1 .* hittingTimeMatrix(T, numUsers);
ct = ht + transpose(ht);

printsimplerec(strcat(outputdir,strcat(num2str(alpha)),'_ht.txt'), ht, index, numUsers, graph);
printsimplerec(strcat(outputdir,strcat(num2str(alpha)),'_ct.txt'), ct, index, numUsers, graph);

% Katz
b = 0.4;
katz = katzMatrix(graph,numUsers,b);
filename = strcat(outputdir,strcat(strcat('KATZ_OUT_',num2str(b)),'.txt'));
printsimplerec(filename, katz,index,numUsers, graph);

% LPI
b = 0.1;
k = 3;
lpi = lpiMatrix(graph,numUsers,b,k);
filename = strcat(outputdir,strcat(strcat(strcat('LPI_OUT_',num2str(b)),strcat('_',num2str(k)))),'.txt');
printsimplerec(filename, lpi,index,numUsers, graph);

% Global LHN Index
phi = 0.4;
lhn2 = globallhnindex(ugraph,numUsers,numUEdges,phi);
filename = strcat(outputdir,strcat(strcat('GlobalLHN_',num2str(phi)),'.txt'));
printsimplerec(filename, lhn2, index, numUsers, graph);
