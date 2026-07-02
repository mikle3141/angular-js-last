import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <section>
      <app-user />
    </section>`,
  styleUrl: './app.css',
  imports: [User]
})

@Component({
 selector: 'app-user',
 template: ` Username: {{ username }} `
})
export class User {
  username = 'John Doe';
}

export class App {}
