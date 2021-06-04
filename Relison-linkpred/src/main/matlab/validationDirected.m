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
indexroute = 'index route';
trainroute = 'training route';
outputdir =  'output route';

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

for alpha = 0.1:0.1:1.0
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
end

% Find the hitting time matrix
ht = -1 .* hittingTimeMatrix(T, numUsers);
% Commute time.
ct = ht + transpose(ht);

% Print the recommendations for ht, ct with PageRank
printsimplerec(strcat(outputdir,'ht.txt'), ht, index, numUsers, graph);
printsimplerec(strcat(outputdir,'ct.txt'), ct, index, numUsers, graph);

% Find Katz matrix
for b = 0.1:0.1:1.0
    for i= 1:3
        if i == 1
            katz = katzMatrix(graph,numUsers,b);
            filename = strcat(outputdir,strcat(strcat('KATZ_OUT_',num2str(b)),'.txt'));
        elseif i == 2
            katz = katzMatrix(transpose(graph),numUsers,b);
            filename = strcat(outputdir,strcat(strcat('KATZ_IN_',num2str(b)),'.txt'));
        else
            katz = katzMatrix(ugraph,numUsers,b);
            filename = strcat(outputdir,strcat(strcat('KATZ_UND_',num2str(b)),'.txt'));
        end
        printsimplerec(filename, katz,index,numUsers, graph);
    end
end

% Find local path index
for b = 0.0:0.1:1.0
    for k = 2:5
        for i= 1:3
            if i == 1
                lpi = lpiMatrix(graph,numUsers,b,k);
                filename = strcat(outputdir,strcat(strcat(strcat('LPI_OUT_',num2str(b)),strcat('_',num2str(k)))),'.txt');
            elseif i == 2
                lpi = lpiMatrix(transpose(graph),numUsers,b,k);
                filename = strcat(outputdir,strcat(strcat(strcat('LPI_IN_',num2str(b)),strcat('_',num2str(k)))),'.txt');
            else
                lpi = lpiMatrix(ugraph,numUsers,b,k);
                filename = strcat(outputdir,strcat(strcat(strcat('LPI_UND_',num2str(b)),strcat('_',num2str(k)))),'.txt');
            end
            printsimplerec(filename, lpi,index,numUsers, graph);
        end
    end
end

% Find global LHN Index
for phi = 0.0:0.1:1.0
    lhn2 = globallhnindex(ugraph,numUsers,numUEdges,phi);
    filename = strcat(outputdir,strcat(strcat('LHNIndex2_',num2str(phi)),'.txt'));
    printsimplerec(filename, lhn2, index, numUsers, graph);
end

% Find Pseudo inverse cosine
pic = pseudoinversecosine(graph,numUsers);
filename = strcat(outputdir,'PIC.txt');
printsimplerec(filename, pic, index, numUsers, graph);

% Find matrix forest
for phi = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
    mf = matrixforest(graph,numUsers,phi);
    filename = strcat(outputdir,strcat(strcat('MatrixForest',num2str(phi)),'.txt'));
    printsimplerec(filename, mf, index, numUsers, graph);
end

% Find the restricted hitting and commute times
hitting = hittingTime(ugraph, numUsers,numUEdges);
commute = commuteTime(ugraph, numUsers, numUEdges);

printsimplerec(strcat(outputdir,'hittingtime.txt'), hitting, index, numUsers, graph);
printsimplerec(strcat(outputdir,'commutetime.txt'), commute, index, numUsers, graph);