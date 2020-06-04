% First, we define the user index, the training network and the output directory
indexroute = 'D:\DefinitiveResults\Recommendation\data\sbs200-follows\sbs200-follows-index.txt';
trainroute = 'D:\DefinitiveResults\Recommendation\data\sbs200-follows\sbs200-follows-train.txt';
outputdir = 'D:\DefinitiveResults\Recommendation\rec\sbs200-follows\sbs200-new\Matlab\';

% Then, we import the index and the training network
index = importdata(indexroute);
train = importdata(trainroute);

% We find the adjacency matrix (and the undirected version)
graph = adjacencyMatrix(index,train);
ugraph = undAdjacencyMatrix(index,train);

% Find the number of users and edges
numUsers = length(index);
numEdges = sum(sum(graph));
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

    printrec(strcat(outputdir,strcat(num2str(alpha)),'_ht.txt'), ht, index, numUsers, graph);
    printrec(strcat(outputdir,strcat(num2str(alpha)),'_ct.txt'), ct, index, numUsers, graph);
end

% Find the hitting time matrix
ht = -1 .* hittingTimeMatrix(T, numUsers);
% Commute time.
ct = ht + transpose(ht);

% Print the recommendations for ht, ct with PageRank
printrec(strcat(outputdir,'ht.txt'), ht, index, numUsers, graph);
printrec(strcat(outputdir,'ct.txt'), ct, index, numUsers, graph);

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
        printrec(filename, katz,index,numUsers, graph);
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
            printrec(filename, lpi,index,numUsers, graph);
        end
    end
end

% Find global LHN Index
for phi = 0.0:0.1:1.0
    lhn2 = lhnindex2(ugraph,numUsers,2.*numEdges,phi);
    filename = strcat(outputdir,strcat(strcat('LHNIndex2_',num2str(phi)),'.txt'));
    printrec(filename, lhn2, index, numUsers, graph);
end

% Find Pseudo inverse cosine
pic = pseudoinversecosine(graph,numUsers);
filename = strcat(outputdir,'PIC.txt');
printrec(filename, pic, index, numUsers, graph);

% Find matrix forest
for phi = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
    mf = matrixforest(graph,numUsers,phi);
    filename = strcat(outputdir,strcat(strcat('MatrixForest',num2str(phi)),'.txt'));
    printrec(filename, mf, index, numUsers, graph);
end

% Find the restricted hitting and commute times
hitting = hittingtime(ugraph, numUsers,2.*numEdges);
commute = commutetime(ugraph, numUsers, 2.*numEdges);

printrec(strcat(outputdir,'hittingtime.txt'), hitting, index, numUsers, graph);
printrec(strcat(outputdir,'commutetime.txt'), commute, index, numUsers, graph);