import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import Utils from 'src/app/app.util';
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: 'app-demo-screen',
  templateUrl: './demo-screen.component.html',
  styleUrls: ['./demo-screen.component.css']
})
export class DemoScreenComponent implements OnInit {

  preRegId: string;
  primaryLang = localStorage.getItem("langCode");

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private translate: TranslateService) 
              {
                this.initiateComponent();
              }

  ngOnInit() {
  }

  private async initiateComponent() {
    this.translate.use(this.primaryLang);
    this.activatedRoute.params.subscribe((param) => {
      this.preRegId = param["appId"];
    });
  }

  onBack() {
    const url = Utils.getURL(this.router.url, "file-upload");
    this.router.navigateByUrl(url + `/${this.preRegId}`);
  }

  onNext() {
    let url = Utils.getURL(this.router.url, "summary");
      this.router.navigateByUrl(url + `/${this.preRegId}/preview`);
  }

}
