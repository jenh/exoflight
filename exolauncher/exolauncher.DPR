program exolauncher;

uses
  Forms,
  Main in 'MAIN.PAS' {ExoLaunchForm};

{$R *.RES}

begin
  Application.CreateForm(TExoLaunchForm, ExoLaunchForm);
  Application.Run;
end.
