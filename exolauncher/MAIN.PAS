
(*

Keys:

Exoflight/<VERSION>
   HomePath - root dir of Exoflight installation
   ClassPath - class path of Exoflight (dirs & jars)
   MainClass - main class to start on interpreter start
   JREBin - \bin\javaw.exe
*)

unit Main;

interface

uses
  SysUtils, Windows, Messages, Classes, Graphics, Controls,
  Forms, Dialogs, StdCtrls, Buttons, ExtCtrls, Menus,
  INIFiles, Registry;

type
  TExoLaunchForm = class(TForm)
    Label1: TLabel;
    resolutionCombo: TComboBox;
    fullScreenBox: TCheckBox;
    depth32: TRadioButton;
    depth16: TRadioButton;
    Label2: TLabel;
    launchBtn: TBitBtn;
    Timer1: TTimer;
    procedure launchBtnClick(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure Timer1Timer(Sender: TObject);
  private
    ini : TRegIniFile;
    registry : TRegistry;
    jrepath,exopath,exebin : string;
    classpath,mainclass : string;
    debug : boolean;

    procedure LoadSettings;
    procedure SaveSettings;
    procedure RunProgram;
    function GetBitDepth : integer;
    function GetScrnWidth : integer;
    function GetScrnHeight : integer;
    function GetFullScreen : boolean;
  end;

var
  ExoLaunchForm: TExoLaunchForm;

const
  EXO_VERSION = '0.8';

implementation

{$R *.DFM}

procedure TExoLaunchForm.FormCreate(Sender: TObject);
var
  pathchars : array[0..4096] of char;
  pathvar : string;
begin
  ini := TRegIniFile.Create('Software\Faster Light\Exoflight\'+EXO_VERSION);
  registry := TRegistry.Create;
  LoadSettings;
end;

procedure TExoLaunchForm.LoadSettings;
begin
  resolutionCombo.text := ini.ReadString('Display', 'Resolution', '1024x768');
  if ini.ReadString('Display', 'BitDepth', '32') = '32' then
    depth32.checked := true
  else
    depth16.checked := true;
  fullScreenBox.checked := ini.ReadString('Display', 'FullScreen', 'true') = 'true';

  registry.RootKey := HKEY_LOCAL_MACHINE;

  if (registry.OpenKey('Software\JavaSoft\Java Runtime Environment\1.4', false)) then
  begin
    jrepath := registry.ReadString('JavaHome');
  end else
  if (registry.OpenKey('Software\JavaSoft\Java Runtime Environment\1.3', false)) then
  begin
    jrepath := registry.ReadString('JavaHome');
  end else
    Application.MessageBox('Could not find Java VM location.', 'Error', ID_OK);
  registry.CloseKey;

  if (registry.OpenKey('Software\Faster Light\Exoflight\'+EXO_VERSION, false)) then
  begin
    exopath := registry.ReadString('HomePath');
    classpath := registry.ReadString('ClassPath');
    mainclass := registry.ReadString('MainClass');
    exebin := registry.ReadString('JREBin');
    debug := registry.ReadBool('Debug');
  end else begin
    exopath := ExtractFilePath(ExpandFileName(Application.ExeName));
    while (exopath[Length(exopath)] = '\') do
          SetLength(exopath, Length(exopath)-1);
    classpath := Format(
              '%0:s\lib\FLCore.jar;' +
              '%0:s\lib\FLGL.jar;' +
              '%0:s\lib\ExoCore.jar;' +
              '%0:s\lib\ExoGUI.jar;' +
              '%0:s\data;' +
              '%0:s\lib\gl4java2.8.2.0-classes.zip;' +
              '%0:s\lib\png.jar',
      [exopath]);
    mainclass := 'com.fasterlight.exo.main.Exoflight';
    exebin := '\bin\javaw.exe';

    Application.MessageBox('Could not find Exoflight in registry.  Setting defaults.', 'Warning', ID_OK);
    registry.OpenKey('Software\Faster Light\Exoflight\'+EXO_VERSION, true);
    registry.WriteString('HomePath', exopath);
    registry.WriteString('ClassPath', classpath);
    registry.WriteString('MainClass', mainclass);
    registry.WriteString('JREBin', exebin);
    registry.WriteBool('Debug', false);
  end;
  registry.CloseKey;
end;

procedure TExoLaunchForm.SaveSettings;
var
  fsstr : string;
begin
  ini.WriteString('Display', 'Resolution', resolutionCombo.text);
  ini.WriteString('Display', 'BitDepth', IntToStr(GetBitDepth));
  if (GetFullScreen) then
    fsstr := 'true'
  else
    fsstr := 'false';
  ini.WriteString('Display', 'FullScreen', fsstr);
end;

function TExoLaunchForm.GetFullScreen : boolean;
begin
  result := fullScreenBox.checked;
end;

function TExoLaunchForm.GetBitDepth : integer;
begin
  if depth16.checked then
     result := 16
  else
     result := 32;
end;

function TExoLaunchForm.GetScrnWidth : integer;
var
  p : integer;
  s : string;
begin
  s := Uppercase(resolutionCombo.text);
  p := Pos('X', s);
  if (p <= 1) then
     raise Exception.Create('Invalid resolution specifier: must be "WIDTHxHEIGHT"');
  result := StrToInt(Copy(s, 1, p-1));
end;

function TExoLaunchForm.GetScrnHeight : integer;
var
  p : integer;
  s : string;
begin
  s := Uppercase(resolutionCombo.text);
  p := Pos('X', s);
  if (p <= 1) then
     raise Exception.Create('Invalid resolution specifier: must be "WIDTHxHEIGHT"');
  result := StrToInt(Copy(s, p+1, length(s)-p));
end;

procedure TExoLaunchForm.RunProgram;
var
  execres : boolean;
  startupInfo : TSTARTUPINFO;
  procInfo : TPROCESSINFORMATION;
  waitres : integer;
  exitcode : integer;
  cmdline : string;
  width,height : integer;
  homedir : string;
begin

  FillChar(startupInfo, sizeof(startupInfo), 0);
  with startupInfo do
  begin
    cb := sizeof(startupInfo);
  end;

  cmdline := jrepath + exebin ;
  if GetFullScreen then
  begin
    cmdline := cmdline + ' -Dexo.fullscreen=true';
  end;
  cmdline := cmdline + ' -Dexo.scrnwidth=' + IntToStr(GetScrnWidth);
  cmdline := cmdline + ' -Dexo.scrnheight=' + IntToStr(GetScrnHeight);
  cmdline := cmdline + ' "-Djava.library.path=' + exopath + '\lib"';
  cmdline := cmdline + ' -cp "' + classpath + '"';
  cmdline := cmdline + ' ' + mainclass;
  cmdline := cmdline + #0;
  homedir := exopath + #0;

  SaveSettings;

  execres := CreateProcess(
    nil,
    @cmdline[1],
    nil, nil, false, NORMAL_PRIORITY_CLASS, nil,
    @homedir[1],
    startupInfo, procInfo);

  waitres := WaitForSingleObject(procInfo.hProcess, INFINITE);
  exitCode := 0;
  case waitres of
    WAIT_FAILED: ;
    WAIT_ABANDONED: ;
    WAIT_TIMEOUT: ;
    else
      GetExitCodeProcess(procInfo.hProcess, exitCode);
  end;

  if not execres or debug then
  begin
    Application.MessageBox(@cmdline[1], 'Could not start VM', IDOK);
  end;

end;

procedure TExoLaunchForm.launchBtnClick(Sender: TObject);
var
  devmode : TDEVMODE;
  fs : boolean;
  changeres : integer;
begin
  fs := GetFullScreen;
  if fs then
  begin
    devmode.dmBitsPerPel := GetBitDepth;
    devmode.dmPelsWidth := GetScrnWidth;
    devmode.dmPelsHeight := GetScrnHeight;
    devmode.dmFields := DM_BITSPERPEL or DM_PELSWIDTH or DM_PELSHEIGHT;
    changeres := ChangeDisplaySettings(devmode, CDS_FULLSCREEN);
    if (changeres <> DISP_CHANGE_SUCCESSFUL) then
       case changeres of
         DISP_CHANGE_RESTART : raise Exception.Create('Need to restart before changing resolution.');
         DISP_CHANGE_FAILED : raise Exception.Create('Resolution change failed.');
         DISP_CHANGE_BADMODE : raise Exception.Create('Resolution and/or color mode not supported.');
       else
         raise Exception.Create('Could not change resolution. (' + IntToStr(changeres) + ')');
       end;
  end;

  Application.Minimize;
  { start timer, we will then wait for the process to be over }
  Timer1.Enabled := true;

end;

procedure TExoLaunchForm.Timer1Timer(Sender: TObject);
begin
  Timer1.Enabled := false;
  RunProgram;

  if GetFullScreen then
  begin
    ChangeDisplaySettings(TDEVMODE(nil^), 0);
  end;
  Close;
end;

end.
