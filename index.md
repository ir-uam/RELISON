 <style>
      body {
          background-color: #fff;
      }

      #title {
          font-weight: bold;
          font-size: 16pt;
      }

      .btn-description {
          display: none;
      }

      #logo {
          width: 50%;
          padding-right: 5px;
          padding-bottom: 5px;
          padding-top: 5px;
      }

      #description {
          width: 50%;
          padding-left: 5px;
      }

      @media (min-width: 384px) {
          #title {
              font-weight: bold;
              font-size: 24pt;
          }

          #logo {
              width: 33%;
              padding-right: 10px;
              padding-bottom: 10px;
              padding-top: 10px;
          }

          #description {
              width: 66%;
              padding-left: 10px;
          }

      }

      @media (min-width: 768px) {
          #title {
              font-weight: bold;
              font-size: 32pt;
          }

          #logo {
              width: 25%;
              padding-right: 10px;
              padding-bottom: 10px;
              padding-top: 10px;
          }

          #description {
              width: 75%;
              padding-left: 10px;
          }

          .btn-description {
              display: initial;
              vertical-align: middle;
              padding-left: 1px;
          }
      }

      .vcenter {
          display: inline-block;
          vertical-align: middle;
          float: none;
      }
    </style> 

 <div class="row">
        <div class="col-xs-12 col-md-8 col-md-offset-2">
          <div class="btn-group btn-group-justified" role="group">
            <a class="btn btn-default" href="https://github.com/ir-uam/RELISON" role="button"><img src="images/octocat-icon.png" alt="See source in GitHub"/><span class="btn-description">GitHub</span></a>
            <a class="btn btn-default" href="https://relison.readthedocs.io" role="button"><img src="images/references.png" alt="Manual"/><span class="btn-description">Manual</span></a>
            <a class="btn btn-default" href="javadoc/" role="button"><img src="images/javadoc.png" alt="See Javadoc"/><span class="btn-description">Javadoc</span></a>
          </div>
        </div>
      </div>
