param(
    [string]$Source = "docs/Book_Exchange_Project_Report.md",
    [string]$Output = "docs/Book_Exchange_Project_Report.docx"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Convert-ToXmlText {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    return [System.Security.SecurityElement]::Escape($Text)
}

function New-ParagraphXml {
    param(
        [string]$Text,
        [ValidateSet("Normal", "Title", "Center", "Heading1", "Heading2", "Caption", "PageBreak")]
        [string]$Kind = "Normal"
    )

    if ($Kind -eq "PageBreak") {
        return "<w:p><w:r><w:br w:type=`"page`"/></w:r></w:p>"
    }

    $safeText = Convert-ToXmlText $Text

    switch ($Kind) {
        "Title" {
            return @"
<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="240" w:after="180"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:b/>
      <w:sz w:val="38"/>
      <w:szCs w:val="38"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
        "Center" {
            return @"
<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:after="90"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:sz w:val="24"/>
      <w:szCs w:val="24"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
        "Heading1" {
            return @"
<w:p>
  <w:pPr>
    <w:spacing w:before="220" w:after="110"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:b/>
      <w:sz w:val="30"/>
      <w:szCs w:val="30"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
        "Heading2" {
            return @"
<w:p>
  <w:pPr>
    <w:spacing w:before="170" w:after="70"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:b/>
      <w:sz w:val="26"/>
      <w:szCs w:val="26"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
        "Caption" {
            return @"
<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="40" w:after="120"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:i/>
      <w:sz w:val="22"/>
      <w:szCs w:val="22"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
        default {
            return @"
<w:p>
  <w:pPr>
    <w:spacing w:after="130" w:line="300" w:lineRule="auto"/>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:sz w:val="24"/>
      <w:szCs w:val="24"/>
    </w:rPr>
    <w:t xml:space="preserve">$safeText</w:t>
  </w:r>
</w:p>
"@
        }
    }
}

function New-ImageParagraphXml {
    param(
        [string]$RelationshipId,
        [long]$WidthEmu,
        [long]$HeightEmu,
        [int]$DocPrId
    )

    return @"
<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="120" w:after="40"/>
  </w:pPr>
  <w:r>
    <w:drawing>
      <wp:inline distT="0" distB="0" distL="0" distR="0">
        <wp:extent cx="$WidthEmu" cy="$HeightEmu"/>
        <wp:effectExtent l="0" t="0" r="0" b="0"/>
        <wp:docPr id="$DocPrId" name="Picture $DocPrId"/>
        <wp:cNvGraphicFramePr>
          <a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/>
        </wp:cNvGraphicFramePr>
        <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
              <pic:nvPicPr>
                <pic:cNvPr id="$DocPrId" name="Picture $DocPrId"/>
                <pic:cNvPicPr/>
              </pic:nvPicPr>
              <pic:blipFill>
                <a:blip r:embed="$RelationshipId"/>
                <a:stretch><a:fillRect/></a:stretch>
              </pic:blipFill>
              <pic:spPr>
                <a:xfrm>
                  <a:off x="0" y="0"/>
                  <a:ext cx="$WidthEmu" cy="$HeightEmu"/>
                </a:xfrm>
                <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
              </pic:spPr>
            </pic:pic>
          </a:graphicData>
        </a:graphic>
      </wp:inline>
    </w:drawing>
  </w:r>
</w:p>
"@
}

if (-not (Test-Path -LiteralPath $Source)) {
    throw "Source file not found: $Source"
}

$sourcePath = (Resolve-Path -LiteralPath $Source).Path
$outputPath = Join-Path (Get-Location) $Output
$outputDir = Split-Path -Parent $outputPath

if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$lines = Get-Content -LiteralPath $sourcePath
$paragraphXml = New-Object System.Collections.Generic.List[string]
$imageEntries = New-Object System.Collections.Generic.List[hashtable]
$docPrId = 100
$imageRelIndex = 2
$sourceDir = Split-Path -Parent $sourcePath
$maxImageWidthEmu = [long](6.1 * 914400)

foreach ($line in $lines) {
    if ($line -eq "<<<PAGEBREAK>>>") {
        $paragraphXml.Add((New-ParagraphXml -Kind "PageBreak" -Text ""))
        continue
    }

    if ($line -match '^!title\s+(.*)$') {
        $paragraphXml.Add((New-ParagraphXml -Kind "Title" -Text $Matches[1]))
        continue
    }

    if ($line -match '^!center\s*(.*)$') {
        $paragraphXml.Add((New-ParagraphXml -Kind "Center" -Text $Matches[1]))
        continue
    }

    if ($line -match '^!figure\s+([^|]+)\|(.*)$') {
        $rawFigurePath = $Matches[1].Trim()
        $caption = $Matches[2].Trim()
        $resolvedFigurePath = if ([System.IO.Path]::IsPathRooted($rawFigurePath)) {
            $rawFigurePath
        } else {
            Join-Path $sourceDir $rawFigurePath
        }

        if (-not (Test-Path -LiteralPath $resolvedFigurePath) -and -not [System.IO.Path]::IsPathRooted($rawFigurePath)) {
            $resolvedFigurePath = Join-Path (Get-Location) $rawFigurePath
        }

        if (-not (Test-Path -LiteralPath $resolvedFigurePath)) {
            throw "Figure not found: $resolvedFigurePath"
        }

        $image = [System.Drawing.Image]::FromFile($resolvedFigurePath)
        try {
            $scale = [Math]::Min(1.0, $maxImageWidthEmu / ([double]($image.Width * 9525)))
            $widthEmu = [long]([Math]::Round($image.Width * 9525 * $scale))
            $heightEmu = [long]([Math]::Round($image.Height * 9525 * $scale))
        }
        finally {
            $image.Dispose()
        }

        $extension = [System.IO.Path]::GetExtension($resolvedFigurePath).ToLowerInvariant()
        $mediaName = "image{0}{1}" -f ($imageEntries.Count + 1), $extension
        $relationshipId = "rId{0}" -f $imageRelIndex
        $imageRelIndex++

        $imageEntries.Add(@{
            SourcePath = $resolvedFigurePath
            MediaName = $mediaName
            RelationshipId = $relationshipId
        })

        $paragraphXml.Add((New-ImageParagraphXml -RelationshipId $relationshipId -WidthEmu $widthEmu -HeightEmu $heightEmu -DocPrId $docPrId))
        $paragraphXml.Add((New-ParagraphXml -Kind "Caption" -Text $caption))
        $docPrId++
        continue
    }

    if ($line -match '^##\s+(.*)$') {
        $paragraphXml.Add((New-ParagraphXml -Kind "Heading1" -Text $Matches[1]))
        continue
    }

    if ($line -match '^###\s+(.*)$') {
        $paragraphXml.Add((New-ParagraphXml -Kind "Heading2" -Text $Matches[1]))
        continue
    }

    if ($line -match '^- (.*)$') {
        $paragraphXml.Add((New-ParagraphXml -Kind "Normal" -Text ("- " + $Matches[1])))
        continue
    }

    if ([string]::IsNullOrWhiteSpace($line)) {
        $paragraphXml.Add("<w:p/>")
        continue
    }

    $paragraphXml.Add((New-ParagraphXml -Kind "Normal" -Text $line))
}

$documentBody = ($paragraphXml -join "`n")

$documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
    xmlns:o="urn:schemas-microsoft-com:office:office"
    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
    xmlns:v="urn:schemas-microsoft-com:vml"
    xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
    xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    xmlns:w10="urn:schemas-microsoft-com:office:word"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
    xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
    xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
    xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
    xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
    mc:Ignorable="w14 wp14">
  <w:body>
$documentBody
    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
"@

$stylesXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="24"/>
        <w:szCs w:val="24"/>
        <w:lang w:val="en-US"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr/>
    </w:pPrDefault>
  </w:docDefaults>
</w:styles>
"@

$contentTypesXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
"@

$relsXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
"@

$documentRelationshipLines = New-Object System.Collections.Generic.List[string]
$documentRelationshipLines.Add('  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>')
foreach ($imageEntry in $imageEntries) {
    $documentRelationshipLines.Add(('  <Relationship Id="{0}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/{1}"/>' -f $imageEntry.RelationshipId, $imageEntry.MediaName))
}
$documentRelsXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$($documentRelationshipLines -join "`n")
</Relationships>
"@

$coreXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:dcmitype="http://purl.org/dc/dcmitype/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Book Exchange SEPM Project Report</dc:title>
  <dc:subject>Software Engineering Project Report</dc:subject>
  <dc:creator>Codex</dc:creator>
  <cp:keywords>Spring Boot, Book Exchange, SEPM, Project Report</cp:keywords>
  <dc:description>Readable Word report for the Book Exchange SEPM repository.</dc:description>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-04-04T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-04-04T00:00:00Z</dcterms:modified>
</cp:coreProperties>
"@

$appXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
    xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Office Word</Application>
  <DocSecurity>0</DocSecurity>
  <ScaleCrop>false</ScaleCrop>
  <HeadingPairs>
    <vt:vector size="2" baseType="variant">
      <vt:variant><vt:lpstr>Title</vt:lpstr></vt:variant>
      <vt:variant><vt:i4>1</vt:i4></vt:variant>
    </vt:vector>
  </HeadingPairs>
  <TitlesOfParts>
    <vt:vector size="1" baseType="lpstr">
      <vt:lpstr>Book Exchange SEPM Project Report</vt:lpstr>
    </vt:vector>
  </TitlesOfParts>
  <Company></Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>
"@

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("book-exchange-report-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempRoot "_rels") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempRoot "docProps") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempRoot "word") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempRoot "word\_rels") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempRoot "word\media") | Out-Null

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "[Content_Types].xml"), $contentTypesXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "_rels\.rels"), $relsXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "docProps\core.xml"), $coreXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "docProps\app.xml"), $appXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "word\document.xml"), $documentXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "word\styles.xml"), $stylesXml, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $tempRoot "word\_rels\document.xml.rels"), $documentRelsXml, $utf8NoBom)

foreach ($imageEntry in $imageEntries) {
    Copy-Item -LiteralPath $imageEntry.SourcePath -Destination (Join-Path $tempRoot ("word\media\" + $imageEntry.MediaName))
}

$zipPath = [System.IO.Path]::ChangeExtension($outputPath, ".zip")
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

$archive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $files = Get-ChildItem -LiteralPath $tempRoot -Recurse -File
    foreach ($file in $files) {
        $relativePath = $file.FullName.Substring($tempRoot.Length).TrimStart('\')
        $entryName = $relativePath -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $entryName) | Out-Null
    }
}
finally {
    $archive.Dispose()
}

Move-Item -LiteralPath $zipPath -Destination $outputPath
Remove-Item -LiteralPath $tempRoot -Recurse -Force

Write-Output "Created $outputPath"
