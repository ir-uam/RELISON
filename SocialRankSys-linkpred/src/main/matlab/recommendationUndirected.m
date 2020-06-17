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

graph = undAdjacencyMatrix(index,train);

numUsers = length(index);
numEdges = sum(sum(graph));

% Hitting and commute time
T = zeros(numUsers, numUsers);
alpha = 0.1;
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

% Katz index
b = 0.1;
katz = katzMatrix(graph,numUsers,b);
filename = strcat(outputdir,strcat(strcat('KATZ_',num2str(b)),'.txt'));
printsimplerec(filename, katz,index,numUsers, graph);

% Local path index
b = 0.1;
k = 3;
lpi = lpiMatrix(graph,numUsers,b,k);
filename = strcat(outputdir,strcat(strcat(strcat('LPI_',num2str(b)),strcat('_',num2str(k)))),'.txt');
printsimplerec(filename, lpi,index,numUsers, graph);

% Global LHN index
phi = 0.1;
lhn2 = globallhnindex(graph,numUsers,numEdges,phi);
filename = strcat(outputdir,strcat(strcat('GlobalLHN_',num2str(phi)),'.txt'));
printsimplerec(filename, lhn2, index, numUsers, graph);